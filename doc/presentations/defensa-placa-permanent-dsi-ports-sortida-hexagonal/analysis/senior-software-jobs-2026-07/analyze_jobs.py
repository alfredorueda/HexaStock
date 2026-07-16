#!/usr/bin/env python3
"""Normalize and code a snapshot of public senior software job adverts.

The script deliberately stores only metadata, URLs, hashes, and coded variables in
the research output. Full advert text remains in the transient raw API snapshot.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import html
import json
import math
import random
import re
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable


SNAPSHOT_DATE = datetime(2026, 7, 16, 23, 59, 59, tzinfo=timezone.utc)
WINDOW_START = datetime(2026, 4, 17, 0, 0, 0, tzinfo=timezone.utc)

SENIOR_TITLE = re.compile(r"(?i)\b(senior|sr\.?|staff|principal|lead)\b|softwareentwickler.?in\s+senior")
ROLE_INCLUDE = re.compile(
    r"(?i)(software\s+(engineer|developer)|softwareentwick|"
    r"back[ -]?end|front[ -]?end|full[ -]?stack|fullstack|"
    r"web\s+(engineer|developer)|mobile\s+(engineer|developer)|"
    r"ios\s+(engineer|developer)|android\s+(engineer|developer)|"
    r"application\s+(software\s+engineer|developer)|"
    r"platform\s+engineer|java\s+developer|\.net\s+(engineer|developer)|"
    r"python\s+(engineer|developer)|ruby\s+(engineer|developer)|"
    r"php\s+(engineer|developer)|react\s+(engineer|developer))"
)
ROLE_EXCLUDE = re.compile(
    r"(?i)\b(manager|director|head\s+of|vice\s+president|vp|chief|"
    r"product\s+manager|program\s+manager|project\s+manager|"
    r"data\s+(engineer|platform)|machine\s+learning|ml\s+engineer|"
    r"ai\s+engineer|analytics\s+engineer|research\s+engineer|"
    r"devops|site\s+reliability|\bsre\b|security\s+engineer|"
    r"qa\b|quality\s+assurance|test\s+engineer|systems?\s+engineer|"
    r"network\s+engineer|support\s+engineer|solutions?\s+engineer|"
    r"sales\s+engineer|customer\s+engineer|mechanical|electrical|"
    r"manufacturing|civil|geotechnical|firmware|embedded|\brf\b|rfic|"
    r"radio\s+frequency|semiconductor|silicon|asic|fpga)\b"
)

PATTERNS: dict[str, list[str]] = {
    "communication": [
        r"(?i)\b(strong|excellent|effective|clear|outstanding|good|exceptional)\s+"
        r"(written\s+(and|&)\s+verbal\s+)?communication\s+skills?\b",
        r"(?i)\b(written|verbal|oral)\s+(and|&)\s+(written|verbal|oral)\b",
        r"(?i)\bcommunicat(?:e|ing)\s+(clearly|effectively|complex|technical|with)\b",
        r"(?i)\b(explain|translate|present)\s+(complex|technical|ideas?|concepts?|trade[- ]?offs?)\b",
        r"(?i)\b(kommunikationsst(?:ä|a)rke|kommunikationsf(?:ä|a)higkeit|kommunikativ)\b",
        r"(?i)\b(ausgepr(?:ä|a)gte|starke|gute)\s+kommunikation\b",
        r"(?i)\b(habilidades?\s+de\s+comunicaci[oó]n|comunicaci[oó]n\s+efectiva|comunicar\s+con\s+claridad)\b",
    ],
    "collaboration": [
        r"(?i)\b(collaborat(?:e|es|ed|ing)\s+(?:closely\s+)?(?:with|across)|"
        r"teamwork|team\s+player|work(?:ing)?\s+closely\s+with|partner(?:ing)?\s+with)\b",
        r"(?i)\bcollaborative\s+(mindset|approach|engineer|team\s+member|working\s+style|by\s+default)\b",
        r"(?i)\bcross[- ]functional\s+(teams?|partners?|collaboration)\b",
        r"(?i)\b(zusammenarbeit\s+mit|teamf(?:ä|a)hig|gemeinsam\s+mit|arbeit\s+im\s+team)\b",
        r"(?i)\b(colabor(?:ar|as|ando|aci[oó]n)\s+con|trabajo\s+en\s+equipo|treball\s+en\s+equip)\b",
    ],
    "stakeholder_alignment": [
        r"(?i)\b(work(?:ing)?|collaborat\w*|partner(?:ing)?|communicat\w*|engag\w*)\s+"
        r"(?:closely\s+|directly\s+)?with\s+(?:internal\s+|external\s+)?"
        r"(stakeholders?|customers?|clients?|users?|product\s+(?:managers?|owners?)|designers?|business\s+teams?)\b",
        r"(?i)\b(understand|translate|balance|align(?:ing)?)\s+(?:the\s+)?"
        r"(business|customer|client|user|product)\s+(needs|requirements|goals|objectives|priorities)\b",
        r"(?i)\btranslate\s+(?:complex\s+)?(business|customer|user|product)\s+\w+\s+into\b",
        r"(?i)\b(zusammenarbeit|abstimmung)\s+mit\s+(kunden|kundinnen|stakeholdern?|fachbereichen?|produktmanagern?)\b",
        r"(?i)\b(kundenbed(?:ü|u)rfnisse|gesch(?:ä|a)ftsanforderungen)\s+(verstehen|(?:ü|u)bersetzen|abstimmen)\b",
        r"(?i)\b(colabor\w*|trabajar)\s+(estrechamente\s+)?con\s+(clientes?|usuarios?|producto|negocio|stakeholders?)\b",
    ],
    "mentoring": [
        r"(?i)\bmentor(?:ing|ed)?\s+(?:and\s+coach(?:ing)?\s+)?"
        r"(engineers?|developers?|junior\w*|team\s+members?|others?|colleagues?|peers?)\b",
        r"(?i)\b(provide|offer|act\s+as)\s+(?:technical\s+)?(mentoring|mentorship|a\s+mentor)\b",
        r"(?i)\bcoach(?:ing|ed)?\s+(engineers?|developers?|junior\w*|team\s+members?|others?|colleagues?|peers?)\b",
        r"(?i)\bguide\s+(junior|other|fellow)\w*|develop\s+others\b",
        r"(?i)\b(mentoring|coachen|kolleg(?:en|innen|:innen)\s+anleiten|wissensweitergabe)\b",
        r"(?i)\b(mentori[az]|acompa(?:ñ|n)ar\s+a\s+(otros|otras|perfiles?))\b",
    ],
    "leadership_influence": [
        r"(?i)\b(technical\s+leadership|lead\s+(technical|architecture|engineering)|set\s+technical\s+direction)\b",
        r"(?i)\binfluence\s+(decisions?|stakeholders?|teams?|strategy|direction|without\s+(?:formal\s+)?authority)\b",
        r"(?i)\b(drive\s+(decisions?|initiatives?|strategy|adoption)|take\s+ownership|own\s+the|accountab\w*)\b",
        r"(?i)\b(technische\s+f(?:ü|u)hrung|verantwortung\s+(?:zu\s+)?(?:ü|u)bernehmen|entscheidungen\s+treffen|ma(?:ß|ss)geblich\s+gestalten)\b",
        r"(?i)\b(liderazgo\s+t[eé]cnico|tomar\s+decisiones|asumir\s+responsabilidad|influir)\b",
    ],
    "feedback_review": [
        r"(?i)\b(code|peer|pull\s+request|pr)\s+reviews?\b",
        r"(?i)\b(give|giving|provide|providing|receive|receiving)\s+(constructive\s+)?feedback\b",
        r"(?i)\b(code[- ]?review|feedbackkultur|konstruktives\s+feedback)\b",
        r"(?i)\b(revisi[oó]n\s+de\s+c[oó]digo|retroalimentaci[oó]n|feedback\s+constructivo)\b",
    ],
    "learning_adaptability": [
        r"(?i)\b(continuous(?:ly)?\s+learn\w*|lifelong\s+learn\w*|learn\s+quickly|growth\s+mindset)\b",
        r"(?i)\b(curious|curiosity|adaptable|adaptability|open[- ]minded|stay\s+(current|up[- ]to[- ]date))\b",
        r"(?i)\b(lernbereitschaft|neugier|neugierig|pers(?:ö|o)nliche\s+weiterentwicklung|anpassungsf(?:ä|a)hig)\b",
        r"(?i)\b(aprendizaje\s+continuo|curiosidad|adaptabilidad|mentalidad\s+de\s+crecimiento)\b",
    ],
    "conflict_negotiation": [
        r"(?i)\b(negotiat\w*|conflict\s+resolution|resolve\s+conflicts?|constructively\s+challenge|healthy\s+debate)\b",
        r"(?i)\b(verhandlungsgeschick|verhandlungen\s+f(?:ü|u)hren|konfliktf(?:ä|a)hig|"
        r"konfliktl(?:ö|o)sung|durchsetzungsverm(?:ö|o)gen)\b",
        r"(?i)\b(negociaci[oó]n|resoluci[oó]n\s+de\s+conflictos|asertividad)\b",
    ],
    "empathy_listening": [
        r"(?i)\b(demonstrate|show|bring|have|with)\s+(?:strong\s+)?empathy\b",
        r"(?i)\b(empathetic|empathic)\s+(communicator|leader|colleague|team\s+member|approach)\b",
        r"(?i)\b(active\s+listen\w*|listen\s+actively)\b",
        r"(?i)\b(empathie|einf(?:ü|u)hlungsverm(?:ö|o)gen|aktiv(?:es)?\s+zuh(?:ö|o)ren)\b",
        r"(?i)\b(empat[ií]a|escucha\s+activa)\b",
    ],
}


@dataclass
class Offer:
    source: str
    source_id: str
    url: str
    title: str
    company: str
    location: str
    published: datetime
    senior_metadata: bool
    text: str


def strip_html(value: str | None) -> str:
    value = value or ""
    value = re.sub(r"(?is)<(script|style).*?>.*?</\1>", " ", value)
    value = re.sub(r"(?s)<[^>]+>", " ", value)
    return re.sub(r"\s+", " ", html.unescape(value)).strip()


def parse_date(value: object) -> datetime:
    if isinstance(value, (int, float)):
        return datetime.fromtimestamp(value, tz=timezone.utc)
    text = str(value or "").strip().replace("Z", "+00:00")
    parsed = datetime.fromisoformat(text)
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def read_json(path: Path) -> object:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def load_offers(raw: Path) -> Iterable[Offer]:
    for path in sorted(raw.glob("arbeitnow-page*.json")):
        for row in read_json(path).get("data", []):
            yield Offer("Arbeitnow", str(row.get("slug", "")), row.get("url", ""), row.get("title", ""),
                        row.get("company_name", ""), row.get("location", ""), parse_date(row.get("created_at")),
                        bool(SENIOR_TITLE.search(row.get("title", ""))), strip_html(row.get("description")))

    for path in sorted(raw.glob("himalayas-page*.json")):
        for row in read_json(path).get("jobs", []):
            seniority = " ".join(row.get("seniority") or [])
            yield Offer("Himalayas", row.get("guid", ""), row.get("guid", ""), row.get("title", ""),
                        row.get("companyName", ""), "; ".join(row.get("locationRestrictions") or ["Remote"]),
                        parse_date(row.get("pubDate")), "senior" in seniority.lower(), strip_html(row.get("description")))

    for path in sorted(raw.glob("jobicy-*.json")):
        for row in read_json(path).get("jobs", []):
            yield Offer("Jobicy", str(row.get("id", "")), row.get("url", ""), row.get("jobTitle", ""),
                        row.get("companyName", ""), row.get("jobGeo", ""), parse_date(row.get("pubDate")),
                        str(row.get("jobLevel", "")).lower() == "senior", strip_html(row.get("jobDescription")))

    for path in sorted(raw.glob("remotive*.json")):
        for row in read_json(path).get("jobs", []):
            yield Offer("Remotive", str(row.get("id", "")), row.get("url", ""), row.get("title", ""),
                        row.get("company_name", ""), row.get("candidate_required_location", "Remote"),
                        parse_date(row.get("publication_date")), bool(SENIOR_TITLE.search(row.get("title", ""))),
                        strip_html(row.get("description")))

    for path in sorted(raw.glob("remoteok*.json")):
        payload = read_json(path)
        for row in payload if isinstance(payload, list) else []:
            if "position" not in row:
                continue
            title = row.get("position", "")
            tags = " ".join(row.get("tags") or [])
            yield Offer("Remote OK", str(row.get("id", "")), row.get("url", ""), title,
                        row.get("company", ""), row.get("location", "Remote"), parse_date(row.get("date")),
                        bool(SENIOR_TITLE.search(title + " " + tags)), strip_html(row.get("description")))

    for path in sorted(raw.glob("themuse-page*.json")):
        for row in read_json(path).get("results", []):
            levels = " ".join(x.get("name", "") for x in row.get("levels", []))
            locations = "; ".join(x.get("name", "") for x in row.get("locations", []))
            yield Offer("The Muse", str(row.get("id", "")), row.get("refs", {}).get("landing_page", ""),
                        row.get("name", ""), row.get("company", {}).get("name", ""), locations,
                        parse_date(row.get("publication_date")), "senior" in levels.lower(), strip_html(row.get("contents")))


def norm(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", value.lower()).strip()


def tier(title: str) -> str:
    if re.search(r"(?i)\b(principal|staff)\b", title):
        return "Staff/Principal"
    if re.search(r"(?i)\b(lead|tech lead|technical lead)\b", title):
        return "Lead"
    return "Senior"


def source_specific_title_ok(offer: Offer) -> bool:
    """Guard against broad/inconsistent taxonomies on generalist boards."""
    if offer.source != "The Muse":
        return True
    explicit_software = re.search(
        r"(?i)\b(software|developer|back[ -]?end|front[ -]?end|full[ -]?stack|fullstack|"
        r"web|mobile|ios|android)\b", offer.title
    )
    return bool(SENIOR_TITLE.search(offer.title) and explicit_software)


def seniority_evidence_ok(offer: Offer) -> bool:
    if SENIOR_TITLE.search(offer.title):
        return True
    years = [int(value) for value in re.findall(
        r"(?i)\b(\d{1,2})\+?\s+years?(?:\s+of)?(?:\s+(?:professional|relevant|hands[- ]on|"
        r"industry|software|development|engineering))?\s+experience\b", offer.text
    )]
    return offer.senior_metadata and bool(years) and max(years) >= 4


def wilson(successes: int, total: int, z: float = 1.959963984540054) -> tuple[float, float]:
    if not total:
        return 0.0, 0.0
    p = successes / total
    denominator = 1 + z * z / total
    centre = (p + z * z / (2 * total)) / denominator
    spread = z * math.sqrt((p * (1 - p) + z * z / (4 * total)) / total) / denominator
    return centre - spread, centre + spread


def percentile(values: list[float], proportion: float) -> float:
    values = sorted(values)
    position = (len(values) - 1) * proportion
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return values[lower]
    return values[lower] + (values[upper] - values[lower]) * (position - lower)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("raw_dir", type=Path)
    parser.add_argument("output_dir", type=Path)
    parser.add_argument("--audit-output", type=Path)
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)

    eligible: list[Offer] = []
    rejection = Counter()
    for offer in load_offers(args.raw_dir):
        if not (WINDOW_START <= offer.published <= SNAPSHOT_DATE):
            rejection["outside_window"] += 1
        elif not offer.senior_metadata and not SENIOR_TITLE.search(offer.title):
            rejection["not_senior"] += 1
        elif not seniority_evidence_ok(offer):
            rejection["insufficient_senior_evidence"] += 1
        elif not source_specific_title_ok(offer):
            rejection["source_title_guard"] += 1
        elif not ROLE_INCLUDE.search(offer.title):
            rejection["not_software_role"] += 1
        elif ROLE_EXCLUDE.search(offer.title):
            rejection["adjacent_or_management_role"] += 1
        elif len(offer.text) < 200:
            rejection["insufficient_description"] += 1
        else:
            eligible.append(offer)

    # First remove exact reposts, then cross-source duplicates with the same
    # company, title and location. Prefer the richest description.
    eligible.sort(key=lambda x: (len(x.text), x.published), reverse=True)
    deduped: list[Offer] = []
    seen_text: set[str] = set()
    seen_role: set[tuple[str, str, str]] = set()
    for offer in eligible:
        text_hash = hashlib.sha256(norm(offer.text).encode()).hexdigest()
        role_key = (norm(offer.company), norm(offer.title), norm(offer.location))
        if text_hash in seen_text or role_key in seen_role:
            rejection["duplicate"] += 1
            continue
        seen_text.add(text_hash)
        seen_role.add(role_key)
        deduped.append(offer)
    deduped.sort(key=lambda x: (x.published, x.source, x.company, x.title), reverse=True)

    fields = ["communication", "collaboration", "stakeholder_alignment", "mentoring",
              "leadership_influence", "feedback_review", "learning_adaptability",
              "conflict_negotiation", "empathy_listening"]
    core_fields = ["communication", "collaboration", "stakeholder_alignment", "mentoring",
                   "leadership_influence", "learning_adaptability", "conflict_negotiation",
                   "empathy_listening"]
    rows: list[dict[str, object]] = []
    offers_by_hash: dict[str, Offer] = {}
    for offer in deduped:
        coded = {name: int(any(re.search(pattern, offer.text) for pattern in patterns))
                 for name, patterns in PATTERNS.items()}
        total = sum(coded.values())
        core_total = sum(coded[name] for name in core_fields)
        text_hash = hashlib.sha256(offer.text.encode()).hexdigest()
        offers_by_hash[text_hash] = offer
        rows.append({
            "source": offer.source,
            "source_id": offer.source_id,
            "url": offer.url,
            "published": offer.published.date().isoformat(),
            "company": offer.company,
            "title": offer.title,
            "seniority_tier": tier(offer.title),
            "location": offer.location,
            "text_sha256": text_hash,
            **coded,
            "any_socioemotional": int(core_total >= 1),
            "two_or_more": int(core_total >= 2),
            "competency_count": total,
            "core_competency_count": core_total,
        })

    csv_fields = ["source", "source_id", "url", "published", "company", "title", "seniority_tier",
                  "location", "text_sha256", *fields, "any_socioemotional", "two_or_more", "competency_count",
                  "core_competency_count"]
    with (args.output_dir / "offers-coded.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=csv_fields)
        writer.writeheader()
        writer.writerows(rows)

    summary_rows: list[dict[str, object]] = []
    n = len(rows)
    for variable in ["any_socioemotional", "two_or_more", *fields]:
        count = sum(int(row[variable]) for row in rows)
        low, high = wilson(count, n)
        summary_rows.append({"variable": variable, "count": count, "n": n,
                             "percent": round(100 * count / n, 1) if n else 0,
                             "ci95_low": round(100 * low, 1), "ci95_high": round(100 * high, 1)})
    with (args.output_dir / "summary.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(summary_rows[0]))
        writer.writeheader()
        writer.writerows(summary_rows)

    company_groups: dict[str, list[dict[str, object]]] = {}
    for row in rows:
        company_groups.setdefault(norm(str(row["company"])), []).append(row)
    balanced_rows: list[dict[str, object]] = []
    rng = random.Random(20260716)
    for variable in ["any_socioemotional", "two_or_more", *fields]:
        company_rates = [sum(int(row[variable]) for row in group) / len(group)
                         for group in company_groups.values()]
        bootstraps = []
        for _ in range(5000):
            draw = [company_rates[rng.randrange(len(company_rates))] for _ in company_rates]
            bootstraps.append(sum(draw) / len(draw))
        balanced_rows.append({
            "variable": variable,
            "companies": len(company_rates),
            "percent": round(100 * sum(company_rates) / len(company_rates), 1),
            "bootstrap_ci95_low": round(100 * percentile(bootstraps, 0.025), 1),
            "bootstrap_ci95_high": round(100 * percentile(bootstraps, 0.975), 1),
        })
    with (args.output_dir / "summary-company-balanced.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(balanced_rows[0]))
        writer.writeheader()
        writer.writerows(balanced_rows)

    tier_rows: list[dict[str, object]] = []
    for tier_name in ["Senior", "Lead", "Staff/Principal"]:
        group = [row for row in rows if row["seniority_tier"] == tier_name]
        for variable in ["any_socioemotional", "two_or_more", *fields]:
            count = sum(int(row[variable]) for row in group)
            low, high = wilson(count, len(group))
            tier_rows.append({"tier": tier_name, "variable": variable, "count": count, "n": len(group),
                              "percent": round(100 * count / len(group), 1),
                              "ci95_low": round(100 * low, 1), "ci95_high": round(100 * high, 1)})
    with (args.output_dir / "summary-by-tier.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(tier_rows[0]))
        writer.writeheader()
        writer.writerows(tier_rows)

    by_source = Counter(row["source"] for row in rows)
    by_tier = Counter(row["seniority_tier"] for row in rows)
    by_company = Counter(norm(str(row["company"])) for row in rows)
    capped_rows = []
    for company_rows in company_groups.values():
        capped_rows.extend(company_rows[:3])
    metadata = {
        "snapshot_date": SNAPSHOT_DATE.date().isoformat(),
        "window_start": WINDOW_START.date().isoformat(),
        "records_loaded": sum(1 for _ in load_offers(args.raw_dir)),
        "included_unique_offers": n,
        "unique_companies": len(by_company),
        "largest_company_cluster": max(by_company.values(), default=0),
        "sensitivity_cap_three_per_company": {
            "offers": len(capped_rows),
            "any_socioemotional_percent": round(100 * sum(int(row["any_socioemotional"]) for row in capped_rows) / len(capped_rows), 1),
            "two_or_more_percent": round(100 * sum(int(row["two_or_more"]) for row in capped_rows) / len(capped_rows), 1),
        },
        "rejections": dict(rejection),
        "by_source": dict(sorted(by_source.items())),
        "by_seniority_tier": dict(sorted(by_tier.items())),
        "maximum_95pct_margin_pp": round(100 * 1.959963984540054 * math.sqrt(0.25 / n), 1) if n else None,
    }
    with (args.output_dir / "metadata.json").open("w", encoding="utf-8") as handle:
        json.dump(metadata, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    if args.audit_output:
        sample = random.Random(20260716).sample(rows, min(60, len(rows)))
        with args.audit_output.open("w", encoding="utf-8") as handle:
            for index, row in enumerate(sample, 1):
                offer = offers_by_hash[str(row["text_sha256"])]
                handle.write(f"\n## {index}. {offer.company} — {offer.title}\n{offer.url}\n")
                flagged = [name for name in fields if row[name]]
                handle.write("Flags: " + (", ".join(flagged) or "none") + "\n")
                for name in flagged:
                    for pattern in PATTERNS[name]:
                        match = re.search(pattern, offer.text)
                        if match:
                            start = max(0, match.start() - 120)
                            end = min(len(offer.text), match.end() + 180)
                            handle.write(f"- {name}: …{offer.text[start:end]}…\n")
                            break

    print(json.dumps(metadata, ensure_ascii=False, indent=2))
    for row in summary_rows:
        print(f"{row['variable']}: {row['count']}/{row['n']} = {row['percent']}% "
              f"(95% CI {row['ci95_low']}–{row['ci95_high']})")


if __name__ == "__main__":
    main()

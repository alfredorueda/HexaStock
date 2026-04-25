# HexaStock reading-edition image: pandoc/extra + a few extra LaTeX
# packages that pandoc/extra does not ship (seqsplit, hyphenat) but that
# we need to make inline `\texttt{...}` content (long Java FQNs, paths)
# break across lines instead of overflowing the right margin.
#
# Build once:
#   docker build --platform linux/amd64 -f scripts/reading-edition/Dockerfile \
#     -t hexastock/reading-edition:latest scripts/reading-edition
#
# (The driver script does this automatically on the first run.)
FROM pandoc/extra:latest

USER root
RUN tlmgr --usermode init-usertree \
 && tlmgr --usermode install seqsplit hyphenat \
 && mktexlsr

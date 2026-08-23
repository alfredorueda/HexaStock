package cat.gencat.agaur.hexastock.adapter.in.telegram.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramUpdateDTOs {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Update(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String text, Chat chat, User from) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(Long id, String username) {}
}


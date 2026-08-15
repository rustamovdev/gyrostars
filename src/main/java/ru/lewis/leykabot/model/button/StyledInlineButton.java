package ru.lewis.leykabot.model.button;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StyledInlineButton extends InlineKeyboardButton {

    @JsonProperty("style")
    private String style; // "primary", "success", "danger"

    @JsonProperty("icon_custom_emoji_id")
    private String iconCustomEmojiId;

    public StyledInlineButton() {
        super("");
    }

    public StyledInlineButton(String text) {
        super(text);
    }

    public static StyledBuilder styledBuilder() {
        return new StyledBuilder();
    }

    public static class StyledBuilder {
        private String text;
        private String callbackData;
        private String url;
        private String style;
        private String iconCustomEmojiId;

        public StyledBuilder text(String text) {
            this.text = text;
            return this;
        }

        public StyledBuilder callbackData(String callbackData) {
            this.callbackData = callbackData;
            return this;
        }

        public StyledBuilder url(String url) {
            this.url = url;
            return this;
        }

        public StyledBuilder style(String style) {
            this.style = style;
            return this;
        }

        public StyledBuilder iconCustomEmojiId(String iconCustomEmojiId) {
            this.iconCustomEmojiId = iconCustomEmojiId;
            return this;
        }

        public StyledInlineButton build() {
            StyledInlineButton button = new StyledInlineButton();
            button.setText(text);
            button.setCallbackData(callbackData);
            button.setUrl(url);
            button.setStyle(style);
            button.setIconCustomEmojiId(iconCustomEmojiId);
            return button;
        }
    }
}

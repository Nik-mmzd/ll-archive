package ru.turikhay.tlauncher.ui.notice;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import ru.turikhay.tlauncher.minecraft.PromotedServer;
import ru.turikhay.tlauncher.minecraft.Server;
import ru.turikhay.util.SwingUtil;
import ru.turikhay.util.U;

import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.net.URL;

public class NoticeDeserializer implements JsonDeserializer<Notice> {

    @Override
    public Notice deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();

        int id = root.get("id").getAsInt();
        int pos;
        if(root.has("pos")) {
            pos = root.get("pos").getAsInt();
        } else {
            pos = -1;
        }
        String text = root.get("text").getAsString();

        text = StringUtils.replace(
                StringUtils.replace(text, "<nobr>", "<span style=\"white-space: nowrap;\">"),
                "</nobr>", "</span>"
        );

        NoticeImage image = parseImage(root, context);
        NoticeAction action = parseAction(id, root, context);

        Notice notice = new Notice(id, pos, text, image, action);
        if(root.has("promoted") && root.get("promoted").getAsBoolean()) {
            notice.setPromoted(true);
        }

        return notice;
    }

    private NoticeImage parseImage(JsonObject root, JsonDeserializationContext context) {
        JsonElement elem = U.requireNotNull(root.get("image"));

        if(elem.isJsonObject()) {
            return parseImageObject(elem.getAsJsonObject(), context);
        }

        String imageSrc = elem.getAsString();
        NoticeImage image;

        createImage:
        {
            base64: {
                BufferedImage base64Image;
                try {
                    base64Image = SwingUtil.base64ToImage(imageSrc);
                } catch (Exception e) {
                    break base64;
                }
                image = new DirectNoticeImage(base64Image);
                break createImage;
            }

            lookIfPredefined: {
                NoticeImage temp = UrlNoticeImage.definedImages.get(imageSrc);
                if(temp == null) {
                    break lookIfPredefined;
                }
                image = temp;
                break createImage;
            }

            /*createUrl: {
                URL url;
                try {
                    url = new URL(imageSrc);
                } catch(Exception e) {
                    break createUrl;
                }
                image = new UrlNoticeImage(url, false);
                break createImage;
            }*/

            throw new IllegalArgumentException("could not parse image: \""+ imageSrc +"\"");
        }

        return image;
    }

    private NoticeImage parseImageObject(JsonObject object, JsonDeserializationContext context) {
        URL url = U.requireNotNull((URL) context.deserialize(object.get("url"), URL.class));
        int width = object.get("width").getAsInt(), height = object.get("height").getAsInt();
        return new UrlNoticeImage(url, width, height);
    }

    private NoticeAction parseAction(int noticeId, JsonObject root, JsonDeserializationContext context) {
        JsonObject actionObject = root.getAsJsonObject("action");
        if(actionObject == null) {
            return null;
        }

        String type = actionObject.get("type").getAsString();

        if("url".equals(type)) {
            return new UrlNoticeAction(actionObject.get("name").getAsString(), U.makeURL(actionObject.get("url").getAsString(), true));
        }

        if("server".equals(type)) {
            return new ServerNoticeAction((PromotedServer) context.deserialize(actionObject.getAsJsonObject("server"), PromotedServer.class), noticeId);
        }

        throw new IllegalArgumentException("unknown action type: " + type);
    }



}

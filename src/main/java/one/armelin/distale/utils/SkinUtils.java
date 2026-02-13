package one.armelin.distale.utils;

import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import one.armelin.distale.DisTale;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static one.armelin.distale.utils.ImageUtils.loadImageFromAsset;
import static one.armelin.distale.utils.Utils.jsonToQueryString;

public class SkinUtils {
    public static BufferedImage getSkinToneGradient(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.bodyCharacteristic != null){
            String skinTone = playerSkin.bodyCharacteristic.split("\\.")[1];
            return loadImageFromAsset(registry.getGradientSets().get("Skin").getGradients().get(skinTone).getTexture());
        }
        return null;
    }

    public static BufferedImage getSkinFaceGrayscale(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.face != null){
            return loadImageFromAsset(registry.getFaces().get(playerSkin.face).getGreyscaleTexture());
        }
        return null;
    }

    public static BufferedImage getSkinFace(PlayerSkin playerSkin){
        if(haveNull(
                playerSkin.bodyCharacteristic,
                playerSkin.face
        )){
            return null;
        }
        BufferedImage faceGrayscale = getSkinFaceGrayscale(playerSkin);
        BufferedImage skinToneGradient = getSkinToneGradient(playerSkin);
        return ImageUtils.applyGradientToSkin(skinToneGradient, faceGrayscale);
    }

    public static BufferedImage getSkinEyes(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.eyes != null){
            return loadImageFromAsset(registry.getEyes().get(playerSkin.eyes.split("\\.")[0]).getGreyscaleTexture());
        }
        return null;
    }

    public static BufferedImage getEyeBackground(PlayerSkin playerSkin){
        BufferedImage eye = getSkinEyes(playerSkin);
        if(eye != null) {
            BufferedImage background = eye.getSubimage(0, 0, 32, 16);
            if(playerSkin.eyes.startsWith("Demonic_Eyes")){
                BufferedImage eyeGradient = getEyeGradient(playerSkin);
                return ImageUtils.applyGradientToSkin(eyeGradient, background);
            }
            return background;
        }
        return null;
    }

    public static BufferedImage getEyeGrayscale(PlayerSkin playerSkin){
        BufferedImage eye = getSkinEyes(playerSkin);
        if(eye != null) {
            return eye.getSubimage(0, 16, 32, 16);
        }
        return null;
    }

    public static BufferedImage getEyeGradient(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.eyes != null){
            String eyeColor = playerSkin.eyes.split("\\.")[1];
            return loadImageFromAsset(registry.getGradientSets().get("Eyes_Gradient").getGradients().get(eyeColor).getTexture());
        }
        return null;
    }

    public static BufferedImage getEyes(PlayerSkin playerSkin){
        BufferedImage eyeGrayscale = getEyeGrayscale(playerSkin);
        BufferedImage eyeGradient = getEyeGradient(playerSkin);
        BufferedImage eyeWithGradient = ImageUtils.applyGradientToSkin(eyeGradient, eyeGrayscale);

        return ImageUtils.resizeByPercentage(eyeWithGradient, 80);
    }

    public static BufferedImage getMouth(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.mouth != null) {
            BufferedImage mouthGrayscale = loadImageFromAsset(registry.getMouths().get(playerSkin.mouth).getGreyscaleTexture());
            if(mouthGrayscale != null) {
                BufferedImage mouths = ImageUtils.applyGradientToSkin(getSkinToneGradient(playerSkin), mouthGrayscale);
                return mouths.getSubimage(3,4,14,4);
            }
        }
        return null;
    }

    public static BufferedImage getHairGradientByColor(String color){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        return loadImageFromAsset(registry.getGradientSets().get("Hair").getGradients().get(color).getTexture());
    }

    public static BufferedImage getEyebrows(PlayerSkin playerSkin){
        CosmeticsModule cosmeticsModule = CosmeticsModule.get();
        CosmeticRegistry registry = cosmeticsModule.getRegistry();

        if(playerSkin.eyebrows != null) {
            BufferedImage eyebrowsGrayscale = loadImageFromAsset(registry.getEyebrows().get(playerSkin.eyebrows.split("\\.")[0]).getGreyscaleTexture());
            if(eyebrowsGrayscale != null) {
                BufferedImage eyebrowsColorGradient = getHairGradientByColor(playerSkin.eyebrows.split("\\.")[1]);
                BufferedImage eyebrows = ImageUtils.applyGradientToSkin(eyebrowsColorGradient, eyebrowsGrayscale);
                if(playerSkin.eyebrows.startsWith("Bushy")){
                    return eyebrows.getSubimage(3,0,15,8);
                }
                if(playerSkin.eyebrows.startsWith("Shaved")){
                    return eyebrows;
                }
                return eyebrows.getSubimage(1,0,16,6);
            }
        }
        return null;
    }

    public static BufferedImage buildFace(PlayerSkin playerSkin){
        if(haveNull(
                playerSkin.bodyCharacteristic,
                playerSkin.face,
                playerSkin.eyes,
                playerSkin.mouth
        )){
            return null;
        }

        BufferedImage skinFace = getSkinFace(playerSkin);
        BufferedImage eyeBackground = getEyeBackground(playerSkin);
        BufferedImage eyes = getEyes(playerSkin);
        BufferedImage l_eye = eyes.getSubimage(0, 0, 13, 13);
        BufferedImage r_eye = eyes.getSubimage(13, 0, 13, 13);
        BufferedImage mouth = getMouth(playerSkin);

        BufferedImage face = ImageUtils.createTransparentBackground(32, 32);
        face = ImageUtils.compose(face, eyeBackground, 0, 7);
        face = ImageUtils.compose(face, l_eye, 2, 10);
        face = ImageUtils.compose(face, r_eye, 17, 10);
        face = ImageUtils.compose(face, skinFace, 1, 0);
        face = ImageUtils.compose(face, mouth, 9, 22);

        if(playerSkin.eyebrows != null){
            BufferedImage l_eyebrow = getEyebrows(playerSkin);
            BufferedImage r_eyebrow = ImageUtils.flipHorizontally(l_eyebrow);
            if(playerSkin.eyebrows.startsWith("Shaved")) {
                BufferedImage eyebrows = l_eyebrow;
                l_eyebrow = eyebrows.getSubimage(5, 20, 11, 2);
                r_eyebrow = ImageUtils.flipHorizontally(eyebrows.getSubimage(5, 8, 11, 2));
                face = ImageUtils.compose(face, l_eyebrow, 3, 10);
                face = ImageUtils.compose(face, r_eyebrow, 18, 10);
            }else{
                if(playerSkin.eyebrows.startsWith("Bushy")){
                    face = ImageUtils.compose(face, l_eyebrow, 1, 6);
                    face = ImageUtils.compose(face, r_eyebrow, 16, 6);
                }else{
                    face = ImageUtils.compose(face, l_eyebrow, 0, 8);
                    face = ImageUtils.compose(face, r_eyebrow, 16, 8);
                }
            }
        }

        return ImageUtils.resizeByPercentage(face.getSubimage(1,0,30,28), 400);
    }

    private static boolean haveNull(Object... items) {
        for (Object item : items) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    public static void generateSkin(PlayerRef ref, @Nullable World world){
        DisTale.LOGGER.atInfo().log("Player Valid? %s", ref.isValid());


        String playerName = ref.getUsername();
        DisTale.LOGGER.atInfo().log("Player %s is missing skin, generating...", playerName);
        Store<EntityStore> store = Objects.requireNonNull(ref.getReference()).getStore();

        CompletableFuture<Void> future = new CompletableFuture<>();

        if(world == null){
            world = store.getExternalData().getWorld();
        }


        world.execute(() -> {
            try {
                PlayerSkinComponent playerSkinComponent = store.getComponent(
                        ref.getReference(),
                        PlayerSkinComponent.getComponentType()
                );

                if(playerSkinComponent != null){
                    PlayerSkin playerSkin = playerSkinComponent.getPlayerSkin();
                    DisTale.LOGGER.atInfo().log("Player %s face image (base64): %s", playerName, ImageUtils.imageToBase64(SkinUtils.buildFace(playerSkin), "png"));
                    //DisTale.playerSkins.put(playerName, SkinUtils.buildFace(playerSkin));
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            DisTale.LOGGER.atSevere().withCause(e).log("Error generating skin for player %s", playerName);
        }
    }

    private static final Pattern PATTERN = Pattern.compile("%(json|base64|query)([*-])\\(([^)]+)\\)%|%(json|base64|query)%");

    private static final Set<String> ALL_FIELDS = new HashSet<>(Arrays.asList(
            "bodyCharacteristic", "underwear", "face", "eyes", "ears", "mouth",
            "facialHair", "haircut", "eyebrows", "pants", "overpants", "undertop",
            "overtop", "shoes", "headAccessory", "faceAccessory", "earAccessory",
            "skinFeature", "gloves", "cape"
    ));

    public static String buildAvatarUrl(String urlTemplate, PlayerSkin skin, UUID uuid, String username) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = PATTERN.matcher(urlTemplate);

        while (matcher.find()) {
            String type = matcher.group(1);
            String operator = matcher.group(2);
            String fieldsStr = matcher.group(3);

            Set<String> fields = parseFields(operator, fieldsStr);
            String replacement = serialize(skin, type, fields);

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        String finalUrl = result.toString();
        finalUrl = finalUrl.replace("%uuid%", uuid.toString());
        finalUrl = finalUrl.replace("%username%", username);

        return finalUrl;
    }

    private static Set<String> parseFields(String operator, String fieldsStr) {
        if (operator == null || fieldsStr == null) {
            return new HashSet<>(ALL_FIELDS);
        }

        String[] fieldArray = fieldsStr.split(",");
        Set<String> specifiedFields = new HashSet<>();
        for (String field : fieldArray) {
            specifiedFields.add(field.trim());
        }

        if ("*".equals(operator)) {
            return specifiedFields;
        } else if ("-".equals(operator)) {
            Set<String> result = new HashSet<>(ALL_FIELDS);
            result.removeAll(specifiedFields);
            return result;
        }

        return new HashSet<>(ALL_FIELDS);
    }

    private static String serialize(PlayerSkin skin, String type, Set<String> fields) {
        JsonObject json = buildJsonObject(skin, fields);
        String jsonString = json.toString();

        return switch (type.toLowerCase()) {
            case "json" -> jsonString;
            case "base64" -> Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(jsonString.getBytes(StandardCharsets.UTF_8));
            case "query" -> jsonToQueryString(jsonString);
            default -> "";
        };
    }

    private static JsonObject buildJsonObject(PlayerSkin skin, Set<String> fields) {
        JsonObject json = new JsonObject();

        if (fields.contains("bodyCharacteristic") && skin.bodyCharacteristic != null) {
            json.addProperty("bodyCharacteristic", skin.bodyCharacteristic);
        }
        if (fields.contains("underwear") && skin.underwear != null) {
            json.addProperty("underwear", skin.underwear);
        }
        if (fields.contains("face") && skin.face != null) {
            json.addProperty("face", skin.face);
        }
        if (fields.contains("eyes") && skin.eyes != null) {
            json.addProperty("eyes", skin.eyes);
        }
        if (fields.contains("ears") && skin.ears != null) {
            json.addProperty("ears", skin.ears);
        }
        if (fields.contains("mouth") && skin.mouth != null) {
            json.addProperty("mouth", skin.mouth);
        }
        if (fields.contains("facialHair") && skin.facialHair != null) {
            json.addProperty("facialHair", skin.facialHair);
        }
        if (fields.contains("haircut") && skin.haircut != null) {
            json.addProperty("haircut", skin.haircut);
        }
        if (fields.contains("eyebrows") && skin.eyebrows != null) {
            json.addProperty("eyebrows", skin.eyebrows);
        }
        if (fields.contains("pants") && skin.pants != null) {
            json.addProperty("pants", skin.pants);
        }
        if (fields.contains("overpants") && skin.overpants != null) {
            json.addProperty("overpants", skin.overpants);
        }
        if (fields.contains("undertop") && skin.undertop != null) {
            json.addProperty("undertop", skin.undertop);
        }
        if (fields.contains("overtop") && skin.overtop != null) {
            json.addProperty("overtop", skin.overtop);
        }
        if (fields.contains("shoes") && skin.shoes != null) {
            json.addProperty("shoes", skin.shoes);
        }
        if (fields.contains("headAccessory") && skin.headAccessory != null) {
            json.addProperty("headAccessory", skin.headAccessory);
        }
        if (fields.contains("faceAccessory") && skin.faceAccessory != null) {
            json.addProperty("faceAccessory", skin.faceAccessory);
        }
        if (fields.contains("earAccessory") && skin.earAccessory != null) {
            json.addProperty("earAccessory", skin.earAccessory);
        }
        if (fields.contains("skinFeature") && skin.skinFeature != null) {
            json.addProperty("skinFeature", skin.skinFeature);
        }
        if (fields.contains("gloves") && skin.gloves != null) {
            json.addProperty("gloves", skin.gloves);
        }
        if (fields.contains("cape") && skin.cape != null) {
            json.addProperty("cape", skin.cape);
        }

        return json;
    }
}

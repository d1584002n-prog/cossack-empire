package ua.cossack.empire;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CossackEmpireClient implements ClientModInitializer {
    public static final String MOD_ID = "cossack_empire";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openMapKey;

    @Override
    public void onInitializeClient() {
        openMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cossack_empire.open_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "key.categories.cossack_empire"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMapKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new StrategyMapScreen());
                }
            }
        });

        LOGGER.info("Козацька імперія завантажена");
    }
}

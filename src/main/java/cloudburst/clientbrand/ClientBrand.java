package cloudburst.clientbrand;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.*;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ClientBrand implements ModInitializer {
	public static final String MODID = "clientbrand";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static HashMap<UUID, String> clientBrands = new HashMap<>();

	@Override
	public void onInitialize() {

		ServerPlayNetworking.registerGlobalReceiver(CustomPayloadC2SPacket.BRAND, (server, player, handler, buf, responseSender) -> {
			try {
				var brand = buf.readString(1024).strip().trim();
				if (!brand.isBlank()) {
					clientBrands.put(player.getUuid(), brand);
					return;
				}
				LOGGER.info("{} brand: {}", player, brand);
			} catch (Exception e) {
				LOGGER.error("Error", e);
			}
			if (player != null) {
				clientBrands.put(player.getUuid(), "unknown");
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			try {
				clientBrands.remove(handler.player.getUuid());
			} catch (Exception e) {
				LOGGER.error("Error", e);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			if (dedicated) {
				dispatcher.register(literal("brand").requires((source) -> {
						return source.hasPermissionLevel(source.getServer().getFunctionPermissionLevel());
					}).then(argument("player", GameProfileArgumentType.gameProfile()).executes(ctx -> {
					for (GameProfile profile : GameProfileArgumentType.getProfileArgument(ctx, "player")) {
						if (profile == null) continue;
						String brand = clientBrands.get(profile.getId());
						if (brand == null) brand = "missing";
						BaseText response = new LiteralText(profile.getName() + " brand is: ");
						var brandText = new LiteralText(brand);
						brandText.setStyle(brandText.getStyle()
								.withColor(brand.equals("vanilla") ? Formatting.GREEN : Formatting.AQUA)
								.withHoverEvent(new HoverEvent(
									HoverEvent.Action.SHOW_TEXT,
									new LiteralText("This may not be entirely correct as modded clients can spoof the brand.")
								)));
						response.append(brandText);
						ctx.getSource().sendFeedback(response, true);
					}

					return SINGLE_SUCCESS;
				})));
			}
		});
	}
}

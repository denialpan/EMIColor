package com.danpan1232.emicolor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;


//import java.io.File;
import java.io.InputStream;
import java.util.*;

@EventBusSubscriber(modid = EMIColor.MOD_ID, value = Dist.CLIENT)
public class AverageColor {

    public static final Map<Block, String> debugTooltipMap = new HashMap<>();

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void preparation, ResourceManager resourceManager, ProfilerFiller profiler) {
                // one tick after, hacky way to solve java memory access fighting with Iris
                Minecraft.getInstance().execute(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

                    AverageColor.debugTooltipMap.clear();

                    Map<ResourceLocation, NativeImage> textureCache = new HashMap<>();
                    Set<ResourceLocation> usedTextures = new HashSet<>();

                    for (Block block : BuiltInRegistries.BLOCK) {
                        BlockState state = block.defaultBlockState();
                        if (state.isAir()) continue;

                        BakedModel model = dispatcher.getBlockModel(state);
                        if (model == null) continue;

                        RandomSource rand = RandomSource.create();
                        ModelData modelData = ModelData.EMPTY;

                        long totalR = 0, totalG = 0, totalB = 0, totalCount = 0;
                        Set<ResourceLocation> alreadyProcessedTextures = new HashSet<>();

                        for (Direction dir : Direction.values()) {
                            List<BakedQuad> quads = model.getQuads(state, dir, rand, modelData, null);
                            if (quads.isEmpty()) continue;

                            for (BakedQuad quad : quads) {
                                TextureAtlasSprite sprite = quad.getSprite();
                                ResourceLocation spriteId = sprite.contents().name();

                                ResourceLocation texturePath = ResourceLocation.fromNamespaceAndPath(
                                        spriteId.getNamespace(), "textures/" + spriteId.getPath() + ".png"
                                );

                                if (!alreadyProcessedTextures.add(texturePath)) continue;

                                NativeImage image = textureCache.get(texturePath);
                                if (image == null) {
                                    Optional<Resource> resOpt = mc.getResourceManager().getResource(texturePath);
                                    if (resOpt.isEmpty()) continue;

                                    try (InputStream stream = resOpt.get().open()) {
                                        image = NativeImage.read(stream);
                                        textureCache.put(texturePath, image);
                                        usedTextures.add(texturePath);
                                    } catch (Exception e) {
                                        System.err.println("Error reading texture: " + texturePath);
                                        e.printStackTrace();
                                        continue;
                                    }
                                }

                                int width = image.getWidth();
                                int height = image.getHeight();

                                for (int y = 0; y < height; y++) {
                                    for (int x = 0; x < width; x++) {
                                        int rgba = image.getPixelRGBA(x, y);
                                        int r = (rgba) & 0xFF;
                                        int g = (rgba >> 8) & 0xFF;
                                        int b = (rgba >> 16) & 0xFF;
                                        int a = (rgba >> 24) & 0xFF;

                                        if (a < 32) continue;

                                        totalR += r;
                                        totalG += g;
                                        totalB += b;
                                        totalCount++;
                                    }
                                }
                            }
                        }

                        if (totalCount > 0) {
                            int avgR = (int)(totalR / totalCount);
                            int avgG = (int)(totalG / totalCount);
                            int avgB = (int)(totalB / totalCount);
                            String hex = String.format("%s: #%02X%02X%02X", block.getName().getString(), avgR, avgG, avgB);
                            AverageColor.debugTooltipMap.put(block, hex);
                            System.out.println("average color " + BuiltInRegistries.BLOCK.getKey(block) + ": " + hex);
                        }
                    }

                    // explicitly free memory
                    for (ResourceLocation tex : usedTextures) {
                        NativeImage img = textureCache.get(tex);
                        if (img != null) img.close();
                    }
                    textureCache.clear();
                    usedTextures.clear();
                });
            }

        });
    }

}

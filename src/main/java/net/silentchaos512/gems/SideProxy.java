package net.silentchaos512.gems;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.silentchaos512.gems.chaos.ChaosSourceCapability;
import net.silentchaos512.gems.client.gui.DebugOverlay;
import net.silentchaos512.gems.client.gui.GuiTypes;
import net.silentchaos512.gems.command.ChaosCommand;
import net.silentchaos512.gems.command.HungryCommand;
import net.silentchaos512.gems.compat.gear.SGearProxy;
import net.silentchaos512.gems.compat.gear.SGearStatHandler;
import net.silentchaos512.gems.config.GemsConfig;
import net.silentchaos512.gems.crafting.altar.AltarRecipeManager;
import net.silentchaos512.gems.crafting.tokenenchanter.TokenEnchanterRecipeManager;
import net.silentchaos512.gems.event.TraitEvents;
import net.silentchaos512.gems.init.*;
import net.silentchaos512.gems.item.TeleporterLinker;
import net.silentchaos512.gems.lib.ColorHandlers;
import net.silentchaos512.gems.lib.chaosbuff.ChaosBuffManager;
import net.silentchaos512.gems.lib.fun.AprilFools;
import net.silentchaos512.gems.lib.soul.GearSoulPart;
import net.silentchaos512.gems.lib.soul.Soul;
import net.silentchaos512.gems.network.Network;
import net.silentchaos512.gems.util.SoulEvents;
import net.silentchaos512.gems.util.gen.GenModels;
import net.silentchaos512.gems.util.gen.GenRecipes;
import net.silentchaos512.gems.world.GemsWorldFeatures;
import net.silentchaos512.lib.inventory.ContainerType;

import java.util.function.BiFunction;

class SideProxy {
    SideProxy() {
        SilentGems.LOGGER.debug("Gems SideProxy init");

        // Detect Silent Gear and load anything needed for compatibility
        SGearProxy.detectSilentGear();
        if (SGearProxy.isLoaded()) {
            SilentGems.LOGGER.info("Register part type {}", GearSoulPart.TYPE);
            MinecraftForge.EVENT_BUS.register(SoulEvents.INSTANCE);
            MinecraftForge.EVENT_BUS.register(TraitEvents.INSTANCE);
        }

        getLifeCycleEventBus().addListener(this::commonSetup);
        getLifeCycleEventBus().addListener(this::imcEnqueue);
        getLifeCycleEventBus().addListener(this::imcProcess);

        getLifeCycleEventBus().addListener(ModBlocks::registerAll);
        getLifeCycleEventBus().addListener(ModEnchantments::registerAll);
        getLifeCycleEventBus().addListener(ModEntities::registerAll);
        getLifeCycleEventBus().addListener(ModItems::registerAll);
        getLifeCycleEventBus().addListener(ModPotions::registerPotions);
        getLifeCycleEventBus().addListener(ModPotions::registerPotionTypes);
        getLifeCycleEventBus().addListener(ModSounds::registerAll);
        getLifeCycleEventBus().addListener(ModTileEntities::registerAll);

        MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
        MinecraftForge.EVENT_BUS.register(Soul.Events.INSTANCE);

        ModLoot.init();
        ModRecipes.init();

        registerContainersCommon();

        GemsConfig.init();
        Network.init();

        if (SilentGems.isDevBuild()) {
            SilentGems.LOGGER.info("Silent's Gems (version {}) detected as a dev build. If this is not a development environment, this is a bug!", SilentGems.getVersion());
        }
    }

    private static IEventBus getLifeCycleEventBus() {
        return FMLJavaModLoadingContext.get().getModEventBus();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        SilentGems.LOGGER.debug("Gems commonSetup");

        ChaosSourceCapability.register();

        if (SGearProxy.isLoaded()) {
            // Register new stats
            MinecraftForge.EVENT_BUS.register(new SGearStatHandler());
        }

        DeferredWorkQueue.runLater(GemsWorldFeatures::addFeaturesToBiomes);

        if (SilentGems.RUN_GENERATORS && SilentGems.isDevBuild()) {
            GenModels.generateModels();
            GenRecipes.generateRecipes();
        }
    }

    private void imcEnqueue(InterModEnqueueEvent event) {
        SilentGems.LOGGER.debug("Gems imcEnqueue");
    }

    private void imcProcess(InterModProcessEvent event) {
        SilentGems.LOGGER.debug("Gems imcProcess");
    }

    private void serverAboutToStart(FMLServerAboutToStartEvent event) {
        IReloadableResourceManager resourceManager = event.getServer().getResourceManager();
        resourceManager.addReloadListener(ChaosBuffManager.INSTANCE);
        resourceManager.addReloadListener(TokenEnchanterRecipeManager.INSTANCE);
        resourceManager.addReloadListener(AltarRecipeManager.INSTANCE);

        CommandDispatcher<CommandSource> dispatcher = event.getServer().getCommandManager().getDispatcher();
        ChaosCommand.register(dispatcher);
        if (SilentGems.isDevBuild()) {
            HungryCommand.register(dispatcher);
        }
    }

    private static void registerContainersCommon() {
        for (GuiTypes type : GuiTypes.values()) {
            //noinspection Convert2MethodRef -- compiler error
            ContainerType.register(type::getContainerType, (tileType, player) ->
                    type.getContainer(tileType, player));
        }
    }

    static class Client extends SideProxy {
        Client() {
            SilentGems.LOGGER.debug("Gems SideProxy.Client init");
            SideProxy.getLifeCycleEventBus().addListener(this::clientSetup);

            MinecraftForge.EVENT_BUS.addListener(ColorHandlers::onBlockColors);
            MinecraftForge.EVENT_BUS.addListener(ColorHandlers::onItemColors);
            MinecraftForge.EVENT_BUS.addListener(TeleporterLinker::renderGameOverlay);

            if (SGearProxy.isLoaded()) {
                MinecraftForge.EVENT_BUS.register(SoulEvents.Client.INSTANCE);
            }

            if (SilentGems.isDevBuild()) {
                MinecraftForge.EVENT_BUS.register(new DebugOverlay());
            }

            if (AprilFools.isRightDay()) {
                SilentGems.LOGGER.info("Registered April Fools Day events");
                MinecraftForge.EVENT_BUS.register(AprilFools.INSTANCE);
            }

            registerContainers();
        }

        private void clientSetup(FMLClientSetupEvent event) {
            SilentGems.LOGGER.debug("Gems clientSetup");
            ModEntities.registerRenderers(event);
            ModTileEntities.registerRenderers(event);
        }

        private static void registerContainers() {
            for (GuiTypes type : GuiTypes.values()) {
                //noinspection Convert2MethodRef -- compiler error
                ContainerType.registerGui(type::getContainerType, (tileType, player) ->
                        type.getGui(tileType, player));
            }

            ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.GUIFACTORY, () -> packet -> {
                ContainerType<?> type = ContainerType.factories.get(packet.getId()).get();
                if (packet.getAdditionalData() != null) type.fromBytes(packet.getAdditionalData());
                //noinspection unchecked
                return ((BiFunction<ContainerType<?>, EntityPlayer, GuiContainer>) ContainerType.guiFactories.get(packet.getId()))
                        .apply(type, Minecraft.getInstance().player);
            });
        }
    }

    static class Server extends SideProxy {
        Server() {
            SilentGems.LOGGER.debug("Gems SideProxy.Server init");
            SideProxy.getLifeCycleEventBus().addListener(this::serverSetup);
        }

        private void serverSetup(FMLDedicatedServerSetupEvent event) {
            SilentGems.LOGGER.debug("Gems serverSetup");
        }
    }
}

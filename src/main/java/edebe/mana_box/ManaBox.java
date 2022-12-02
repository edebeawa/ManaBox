package edebe.mana_box;

import edebe.mana_box.common.BlockItemManaBox;
import edebe.mana_box.common.BlockManaBox;
import edebe.mana_box.common.TileManaBox;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ManaBox.MODID)
public class ManaBox {
    private static final String PROTOCOL_VERSION = "1";

    public static final String MODID = "mana_box";
    public static final Logger LOGGER = LogManager.getLogger(ManaBox.MODID);
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(ManaBox.MODID, ManaBox.MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public ManaBox() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MoreLensFMLBusEvents(this));

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(this);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ()->()->modBus.addListener(this::clientSetup));
        modBus.addGenericListener(Block.class, this::registerBlocks);
        modBus.addGenericListener(Item.class, this::registerItems);
        modBus.addGenericListener(TileEntityType.class, this::registerTiles);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        RenderTypeLookup.setRenderLayer(MANA_BOX_BLOCK, RenderType.getTranslucent());
        Minecraft.getInstance().getBlockColors().register((state, reader, pos, color) -> {
            if (reader != null && pos != null) {
                TileEntity tileEntity = reader.getTileEntity(pos);
                return tileEntity instanceof TileManaBox ? ((TileManaBox) tileEntity).getColor().getColorValue() : -1;
            } else {
                return -1;
            }
        }, MANA_BOX_BLOCK);
        Minecraft.getInstance().getItemColors().register((stack, color) -> BlockItemManaBox.getColor(stack).getColorValue(), MANA_BOX_ITEM);
    }

    public static final Block MANA_BOX_BLOCK = new BlockManaBox();

    public void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        registry.register(MANA_BOX_BLOCK.setRegistryName(new ResourceLocation(MODID, "mana_box")));
    }

    public static final TileEntityType<TileManaBox> MANA_BOX_TILE_ENTITY_TYPE = TileEntityType.Builder.create(TileManaBox::new, MANA_BOX_BLOCK).build(null);

    public void registerTiles(RegistryEvent.Register<TileEntityType<?>> event) {
        IForgeRegistry<TileEntityType<?>> registry = event.getRegistry();
        registry.register(MANA_BOX_TILE_ENTITY_TYPE.setRegistryName(new ResourceLocation(MODID, "mana_box")));
    }

    public static final Item MANA_BOX_ITEM = new BlockItemManaBox();

    private void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.register(MANA_BOX_ITEM.setRegistryName(new ResourceLocation(MODID, "mana_box")));
    }

    private static class MoreLensFMLBusEvents {
        private final ManaBox parent;

        MoreLensFMLBusEvents(ManaBox parent) {
            this.parent = parent;
        }
    }
}

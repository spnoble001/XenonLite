package xenon.dev.utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;
import xenon.dev.modules.Module;

@SideOnly(Side.CLIENT)
public final class RenderUtils {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Map<Integer, Boolean> GL_CAP_MAP = new HashMap<Integer, Boolean>();

    private RenderUtils() {
    }

    public static void drawFilledBox(AxisAlignedBB box) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableBlend();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        addFace(renderer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
        addFace(renderer, box.maxX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ);
        addHorizontalFace(renderer, box.minX, box.minY, box.minZ, box.maxX, box.maxZ);
        addHorizontalFace(renderer, box.minX, box.maxY, box.maxZ, box.maxX, box.minZ);
        addSideFace(renderer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.minZ);
        addSideFace(renderer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        tessellator.draw();
        GlStateManager.disableBlend();
    }

    private static void addFace(WorldRenderer renderer, double x1, double y1, double z, double x2, double y2, double ignoredZ) {
        vertex(renderer, x1, y1, z); vertex(renderer, x1, y2, z);
        vertex(renderer, x2, y2, z); vertex(renderer, x2, y1, z);
    }

    private static void addHorizontalFace(WorldRenderer renderer, double x1, double y, double z1, double x2, double z2) {
        vertex(renderer, x1, y, z1); vertex(renderer, x2, y, z1);
        vertex(renderer, x2, y, z2); vertex(renderer, x1, y, z2);
    }

    private static void addSideFace(WorldRenderer renderer, double x, double y1, double z1, double ignoredX, double y2, double z2) {
        vertex(renderer, x, y1, z1); vertex(renderer, x, y2, z1);
        vertex(renderer, x, y2, z2); vertex(renderer, x, y1, z2);
    }

    private static void vertex(WorldRenderer renderer, double x, double y, double z) {
        renderer.pos(x, y, z).color(0.0F, 1.0F, 1.0F, 0.25F).endVertex();
    }

    public static void drawRect(float x, float y, float x2, float y2, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        glColor(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x2, y); GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y2); GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }

    public static void drawBorderedRect(float x, float y, float x2, float y2, float width, int border, int fill) {
        drawRect(x, y, x2, y2, fill);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        glColor(border);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y); GL11.glVertex2f(x, y2);
        GL11.glVertex2f(x2, y2); GL11.glVertex2f(x2, y);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }

    public static void drawLoadingCircle(float x, float y) {
        for (int i = 0; i < 4; i++) {
            int rotation = (int) (System.nanoTime() / 5000000L * i % 360L);
            drawCircle(x, y, i * 10, rotation - 180, rotation, new XObject<Color>(Color.WHITE));
        }
    }

    public static void drawOutlinedBox(AxisAlignedBB box) {
        GL11.glBegin(GL11.GL_LINES);
        line(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ);
        line(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ);
        line(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ);
        line(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ);
        line(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ);
        line(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ);
        line(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ);
        line(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ);
        line(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ);
        line(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
        line(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ);
        line(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ);
        GL11.glEnd();
    }

    private static void line(double x1, double y1, double z1, double x2, double y2, double z2) {
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
    }

    public static void drawCircle(float x, float y, float radius, int start, int end, XObject<Color> color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        glColor(color.get());
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (float angle = end; angle >= start; angle -= 4.0F) {
            GL11.glVertex2d(x + Math.cos(Math.toRadians(angle)) * radius,
                    y + Math.sin(Math.toRadians(angle)) * radius);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawFilledCircle(float x, float y, float radius, XObject<Color> color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        glColor(color.get());
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= 50; i++) {
            double angle = Math.PI * 2.0D * i / 50.0D;
            GL11.glVertex2d(x + radius * Math.sin(angle), y + radius * Math.cos(angle));
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawImage(ResourceLocation image, int x, int y, int width, int height) {
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MC.getTextureManager().bindTexture(image);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, width, height, width, height);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    public static void glColor(Object value) {
        Color color = (Color) value;
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F,
                color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
    }

    private static void glColor(int color) {
        GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F,
                (color & 255) / 255.0F, (color >> 24 & 255) / 255.0F);
    }

    public static int getPing(EntityPlayer player) {
        if (player == null || MC.getNetHandler() == null) {
            return 0;
        }
        NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(player.getUniqueID());
        return info == null ? 0 : info.getResponseTime();
    }

    public static Vec3 getLookPos() {
        EntityPlayerSP player = MC.thePlayer;
        return player == null ? new Vec3(0.0D, 0.0D, 0.0D) : player.getLookVec();
    }

    public static void renderLivingLabel(EntityPlayer player, XObject<String> text, double x, double y,
                                         double z, double scale, XObject<Boolean> armor, XObject<Boolean> name) {
        if (player == null || text == null) {
            return;
        }
        y += 0.5999D;
        double distanceSq = player.getDistanceSqToEntity(MC.getRenderManager().livingPlayer);
        FontRenderer font = MC.fontRendererObj;
        float renderScale = 0.016666668F * (0.6F + player.getEyeHeight());
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + player.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-MC.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(MC.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-renderScale, -renderScale, renderScale);
        float distanceScale = (float) (1.0D + distanceSq / scale);
        GlStateManager.scale(distanceScale, distanceScale, distanceScale);
        if (Boolean.TRUE.equals(name.get())) {
            int half = font.getStringWidth(text.get()) / 2 + 2;
            drawRect(-half - 1, -2, half + 1, 9, 0x80000000);
            font.drawString(text.get(), -font.getStringWidth(text.get()) / 2, 0, Color.WHITE.getRGB());
        }
        if (Boolean.TRUE.equals(armor.get())) {
            renderArmor(player);
        }
        GlStateManager.popMatrix();
    }

    private static void renderArmor(EntityPlayer player) {
        int x = 0;
        for (ItemStack stack : player.inventory.armorInventory) {
            if (stack != null) x -= 8;
        }
        if (player.getHeldItem() != null) {
            x -= 8;
            renderItemStack(player.getHeldItem().copy(), x, -20);
            x += 16;
        }
        for (int index = 3; index >= 0; index--) {
            ItemStack stack = player.inventory.armorInventory[index];
            if (stack != null) {
                renderItemStack(stack, x, -20);
                x += 16;
            }
        }
    }

    private static void renderItemStack(ItemStack stack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        MC.getRenderItem().zLevel = -150.0F;
        MC.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        MC.getRenderItem().renderItemOverlays(MC.fontRendererObj, stack, x, y);
        MC.getRenderItem().zLevel = 0.0F;
        GlStateManager.scale(0.5D, 0.5D, 0.5D);
        renderEnchantText(stack, x, y);
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    private static void renderEnchantText(ItemStack stack, int x, int y) {
        int lineY = y - 24;
        if (stack.getEnchantmentTagList() != null && stack.getEnchantmentTagList().tagCount() >= 6) {
            MC.fontRendererObj.drawString("god", x * 2, lineY, Color.RED.getRGB());
            return;
        }
        if (stack.getItem() instanceof ItemArmor) {
            lineY = enchant(stack, Enchantment.protection, "pr", x, lineY);
            lineY = enchant(stack, Enchantment.projectileProtection, "pp", x, lineY);
            lineY = enchant(stack, Enchantment.blastProtection, "bp", x, lineY);
            lineY = enchant(stack, Enchantment.fireProtection, "fp", x, lineY);
            lineY = enchant(stack, Enchantment.thorns, "t", x, lineY);
        } else if (stack.getItem() instanceof ItemBow) {
            lineY = enchant(stack, Enchantment.power, "po", x, lineY);
            lineY = enchant(stack, Enchantment.punch, "pu", x, lineY);
            lineY = enchant(stack, Enchantment.flame, "f", x, lineY);
        } else if (stack.getItem() instanceof ItemSword) {
            lineY = enchant(stack, Enchantment.sharpness, "sh", x, lineY);
            lineY = enchant(stack, Enchantment.knockback, "kn", x, lineY);
            lineY = enchant(stack, Enchantment.fireAspect, "f", x, lineY);
        } else if (stack.getItem() instanceof ItemTool) {
            lineY = enchant(stack, Enchantment.efficiency, "eff", x, lineY);
            lineY = enchant(stack, Enchantment.fortune, "fo", x, lineY);
            lineY = enchant(stack, Enchantment.silkTouch, "st", x, lineY);
        }
        enchant(stack, Enchantment.unbreaking, "ub", x, lineY);
        if (stack.getItem() == Items.golden_apple && stack.hasEffect()) {
            MC.fontRendererObj.drawString("god", x * 2, lineY + 8, 0xFFEFEFEF);
        }
    }

    private static int enchant(ItemStack stack, Enchantment enchantment, String prefix, int x, int y) {
        int level = EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, stack);
        if (level > 0) {
            MC.fontRendererObj.drawString(prefix + level, x * 2, y, 0xFFEFEFEF);
            return y + 8;
        }
        return y;
    }

    public static void resetCaps() {
        for (Map.Entry<Integer, Boolean> entry : GL_CAP_MAP.entrySet()) {
            setGlState(entry.getKey(), entry.getValue());
        }
        GL_CAP_MAP.clear();
    }

    public static void enableGlCap(int cap) { setGlCap(cap, true); }
    public static void enableGlCap(int... caps) { for (int cap : caps) setGlCap(cap, true); }
    public static void disableGlCap(int cap) { setGlCap(cap, false); }
    public static void disableGlCap(int... caps) { for (int cap : caps) setGlCap(cap, false); }

    public static void setGlCap(int cap, boolean state) {
        if (!GL_CAP_MAP.containsKey(cap)) {
            GL_CAP_MAP.put(cap, GL11.glGetBoolean(cap));
        }
        setGlState(cap, state);
    }

    public static void setGlState(int cap, boolean state) {
        if (state) GL11.glEnable(cap); else GL11.glDisable(cap);
    }

    public static void drawString(Module module, XObject<Float> x, XObject<Float> y,
                                  XObject<Object> color, boolean shadow) {
        drawFiltered(module.getCharacters(), x.get(), y.get(), ((Color) color.get()).getRGB(), shadow, false);
    }

    public static void drawString(String text, float x, float y, Color color) {
        MC.fontRendererObj.drawString(text, x, y, color.getRGB(), false);
        GlStateManager.resetColor();
    }

    public static void drawString(XObject<char[]> text, float x, float y, Color color) {
        drawFiltered(text.get(), x, y, color.getRGB(), false, false);
    }

    public static void drawString2(String text, float x, float y, Color color) {
        drawFiltered((text == null ? "dramatically#4536" : text).toCharArray(), x, y,
                color.getRGB(), false, true);
    }

    private static void drawFiltered(char[] characters, float x, float y, int color, boolean shadow, boolean hash) {
        float position = x;
        for (char character : characters) {
            if (character == ' ' || (hash && character == '#')
                    || (character != '&' && StringUtils.isAlphanumeric(String.valueOf(character)))) {
                MC.fontRendererObj.drawString(String.valueOf(character), position, y, color, shadow);
                position += MC.fontRendererObj.getCharWidth(character);
            }
        }
        GlStateManager.resetColor();
    }

    public static int getStringWidth(Module module) {
        return getStringWidth(module.getCharacters());
    }

    public static int getStringWidth(char[] characters) {
        int width = 0;
        for (char character : characters) {
            if (character == ' ' || (character != '&' && StringUtils.isAlphanumeric(String.valueOf(character)))) {
                width += MC.fontRendererObj.getCharWidth(character);
            }
        }
        return width;
    }
}

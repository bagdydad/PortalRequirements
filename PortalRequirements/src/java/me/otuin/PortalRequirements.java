package me.otuin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PortalRequirements extends JavaPlugin implements Listener {
    private ShapedRecipe netherRecipe = null;
    private ShapedRecipe endRecipe = null;
    private NamespacedKey netherKey;
    private NamespacedKey endKey;
    private NamespacedKey savedEffectsKey;
    private Material netherUnlockMat;
    private Material endUnlockMat;
    private Material netherScrollMat;
    private Material endScrollMat;

    Sound[] netherrandomsounds = {
            Sound.ENTITY_VEX_AMBIENT,
            Sound.ENTITY_VEX_DEATH,
            Sound.ENTITY_VEX_CHARGE
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        netherKey = new NamespacedKey(this, "prNether");
        endKey = new NamespacedKey(this, "prEnd");
        savedEffectsKey = new NamespacedKey(this, "saved_effects");

        loadMaterialsFromConfig();
        recipeRegister();
    }

    @Override
    public void onDisable() {
        recipeRemover();
    }

    private void loadMaterialsFromConfig() {
        netherUnlockMat = Material.matchMaterial(getConfig().getString("triggers.nether_unlock_item", "OBSIDIAN"));
        endUnlockMat = Material.matchMaterial(getConfig().getString("triggers.end_unlock_item", "ENDER_EYE"));
        netherScrollMat = Material.matchMaterial(getConfig().getString("nether_scroll.item", "FLOWER_BANNER_PATTERN"));
        endScrollMat = Material.matchMaterial(getConfig().getString("end_scroll.item", "GLOBE_BANNER_PATTERN"));
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private ShapedRecipe createRecipe(String configPath, NamespacedKey key, Material scrollMat) {
        if (scrollMat == null) scrollMat = Material.PAPER;
        ItemStack item = new ItemStack(scrollMat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(getConfig().getString(configPath + ".name")));

        List<String> lore = new ArrayList<>();
        for (String l : getConfig().getStringList(configPath + ".lore")) {
            lore.add(color(l));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);

        String enchName = getConfig().getString(configPath + ".enchantment");
        int enchLvl = getConfig().getInt(configPath + ".enchantment_level");
        if (enchName != null && !enchName.isEmpty()) {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchName.toLowerCase(Locale.ENGLISH)));
            if (ench == null) ench = Enchantment.getByName(enchName.toUpperCase(Locale.ENGLISH));
            if (ench != null) item.addUnsafeEnchantment(ench, enchLvl);
        }

        ShapedRecipe recipe = new ShapedRecipe(key, item);
        List<String> shape = getConfig().getStringList(configPath + ".recipe.shape");
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        ConfigurationSection ingredients = getConfig().getConfigurationSection(configPath + ".recipe.ingredients");
        if (ingredients != null) {
            for (String ingKey : ingredients.getKeys(false)) {
                Material ingMat = Material.matchMaterial(ingredients.getString(ingKey));
                if (ingMat != null) {
                    recipe.setIngredient(ingKey.charAt(0), ingMat);
                }
            }
        }
        return recipe;
    }

    private void recipeRegister() {
        netherRecipe = createRecipe("nether_scroll", netherKey, netherScrollMat);
        Bukkit.addRecipe(netherRecipe);
        Bukkit.broadcastMessage("Nether recipe added!");
        endRecipe = createRecipe("end_scroll", endKey, endScrollMat);
        Bukkit.addRecipe(endRecipe);
        Bukkit.broadcastMessage("End recipe added!");
    }

    private void recipeRemover() {
        Bukkit.removeRecipe(netherKey);
        Bukkit.removeRecipe(endKey);
        Bukkit.broadcastMessage("Recipes removed!");
    }

    private void unlockRecipe(Player player, NamespacedKey key) {
        if (!player.hasDiscoveredRecipe(key)) {
            player.discoverRecipe(key);
        }
    }

    private void applyConfigEffects(Player player, String configPath) {
        int uDur = getConfig().getInt(configPath + ".unluck_effect.duration", 12000);
        int uAmp = getConfig().getInt(configPath + ".unluck_effect.amplifier", 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, uDur, uAmp));
        List<Map<?, ?>> extraEffects = getConfig().getMapList(configPath + ".extra_effects");
        for (Map<?, ?> map : extraEffects) {
            String typeStr = (String) map.get("type");
            Number durNum = (Number) map.get("duration");
            Number ampNum = (Number) map.get("amplifier");
            int dur = durNum != null ? durNum.intValue() : 12000;
            int amp = ampNum != null ? ampNum.intValue() : 1;

            PotionEffectType pType = PotionEffectType.getByName(typeStr);
            if (pType != null) {
                player.addPotionEffect(new PotionEffect(pType, dur, amp));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Material type = event.getItem().getItemStack().getType();
        if (type == netherUnlockMat && netherUnlockMat != null) {
            unlockRecipe(player, netherKey);
        } else if (type == endUnlockMat && endUnlockMat != null) {
            unlockRecipe(player, endKey);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if ((currentItem != null && currentItem.getType() == netherUnlockMat) || (cursorItem != null && cursorItem.getType() == netherUnlockMat)) {
            unlockRecipe(player, netherKey);
        }
        if ((currentItem != null && currentItem.getType() == endUnlockMat) || (cursorItem != null && cursorItem.getType() == endUnlockMat)) {
            unlockRecipe(player, endKey);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL && !player.hasPermission("portalrequirements.nether")) {
            event.setCancelled(true);
        } else if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL && !player.hasPermission("portalrequirements.end")) {
            event.setCancelled(true);
            Location loc = player.getLocation();
            int[][] offsets = {{0,1}, {0,-1}, {1,0}, {-1,0}, {1,1}, {1,-1}, {-1,1}, {-1,-1}, {0,2}, {0,-2}, {2,0}, {-2,0}};

            for (int[] offset : offsets) {
                if (loc.getWorld().getBlockAt(loc.getBlockX() + offset[0], loc.getBlockY(), loc.getBlockZ() + offset[1]).getType() == Material.END_PORTAL_FRAME) {
                    player.teleport(new Location(loc.getWorld(), loc.getBlockX() + offset[0] + 0.5, loc.getBlockY() + 1, loc.getBlockZ() + offset[1] + 0.5, loc.getYaw(), loc.getPitch()));
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo().getWorld().getName().contains("world_nether")) {
            if (event.getPlayer().hasPermission("portalrequirements.nether")) return;
            event.setCancelled(true);
        } else if (event.getTo().getWorld().getName().contains("world_the_end")) {
            if (event.getPlayer().hasPermission("portalrequirements.end")) return;
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getPlayer() == null) return;
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (!event.getHand().equals(EquipmentSlot.HAND)) return;
        Material type = event.getItem().getType();
        if (type != netherScrollMat && type != endScrollMat) return;

        Player player = event.getPlayer();
        int nether = event.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(netherKey, PersistentDataType.INTEGER, 0);
        int end = event.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(endKey, PersistentDataType.INTEGER, 0);
        if (nether == 1) {
            if (!player.hasPermission("portalrequirements.nether")) {
                player.getInventory().removeItem(event.getItem());
                for (int i = 0; i < 20; i++) {
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        ThreadLocalRandom random = ThreadLocalRandom.current();
                        double rx = random.nextDouble(-5.0, 5.0);
                        double rz = random.nextDouble(-5.0, 5.0);
                        Location randomLoc = player.getLocation().clone().add(rx, 1.0, rz);

                        Sound sound = netherrandomsounds[random.nextInt(netherrandomsounds.length)];
                        for (int j = 0; j < 3; j++) {
                            float volume = (float) random.nextDouble(0.6, 1.2);
                            float pitch = (float) random.nextDouble(0.3, 1.8);
                            player.playSound(randomLoc, sound, volume, pitch);
                        }
                    }, i * 10L);
                }
                player.spawnParticle(Particle.COPPER_FIRE_FLAME, player.getLocation().add(0, 1, 0), 45);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.4f);
                getServer().dispatchCommand(getServer().getConsoleSender(), "lp user " + player.getName() + " permission set portalrequirements.nether");
                player.sendMessage(color(getConfig().getString("messages.nether_line_1")));
                player.sendMessage(color(getConfig().getString("messages.nether_line_2")));
                applyConfigEffects(player, "nether_scroll");
            } else {
                player.sendMessage(color(getConfig().getString("messages.already_read")));
            }

        } else if (end == 1) {
            if (player.hasPermission("portalrequirements.nether")) {
                if (!player.hasPermission("portalrequirements.end")) {
                    player.getInventory().removeItem(event.getItem());
                    player.spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 65);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.4f);
                    getServer().dispatchCommand(getServer().getConsoleSender(), "lp user " + player.getName() + " permission set portalrequirements.end");
                    player.sendMessage(color(getConfig().getString("messages.end_line_1")));
                    player.sendMessage(color(getConfig().getString("messages.end_line_2")));
                    applyConfigEffects(player, "end_scroll");
                } else {
                    player.sendMessage(color(getConfig().getString("messages.already_read")));
                }
            } else {
                player.sendMessage(color(getConfig().getString("messages.need_nether_first")));
            }
        }
    }

    @EventHandler
    private void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getPlayer() == null || event.getItem() == null) return;
        if (!event.getItem().getType().equals(Material.MILK_BUCKET)) return;
        Player player = event.getPlayer();
        if (player.hasPotionEffect(PotionEffectType.UNLUCK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PotionEffect unluck = player.getPotionEffect(PotionEffectType.UNLUCK);
        if (unluck == null) return;
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        StringBuilder effectsStr = new StringBuilder();

        for (PotionEffect effect : player.getActivePotionEffects()) {
            effectsStr.append(effect.getType().getName()).append(":").append(effect.getDuration()).append(":").append(effect.getAmplifier()).append(";");
        }
        pdc.set(savedEffectsKey, PersistentDataType.STRING, effectsStr.toString());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            if (pdc.has(savedEffectsKey, PersistentDataType.STRING)) {
                String effectsStr = pdc.get(savedEffectsKey, PersistentDataType.STRING);
                if (effectsStr != null) {
                    for (String eff : effectsStr.split(";")) {
                        if (eff.isEmpty()) continue;
                        String[] parts = eff.split(":");
                        if (parts.length == 3) {
                            PotionEffectType type = PotionEffectType.getByName(parts[0]);
                            if (type != null) {
                                player.addPotionEffect(new PotionEffect(type, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                            }
                        }
                    }
                }
                pdc.remove(savedEffectsKey);
            }
        }, 5L);
    }
}
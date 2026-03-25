package me.otuin;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PortalRequirements extends JavaPlugin implements Listener {

	ShapedRecipe netherRecipe = null;
	ShapedRecipe endRecipe = null;

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		recipeRegister();
	}

	@Override
	public void onDisable() {
		recipeRemover();
	}

	private void recipeRegister() {

		NamespacedKey netherKey = new NamespacedKey(this, "prNether");
		ItemStack netherItem = new ItemStack(Material.GLOBE_BANNER_PATTERN);
		ItemMeta netherItemMeta = netherItem.getItemMeta();
		netherItemMeta.setDisplayName(ChatColor.RED + "Cehennem Parşömeni");
		netherItemMeta.setLore(Arrays.asList(ChatColor.RED + "Cehennem'in kapılarını açan kadim parşömen.", ChatColor.RED + "Bu parşömeni okumanın bedeli çok ağırdır.", ChatColor.RED + "Ne yaptığını bilmeyenler bu parşömene dokunmamalıdır!", ChatColor.RED + "Parşömeni okumak için sağ tıkla."));
		netherItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		netherItemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		netherItemMeta.getPersistentDataContainer().set(netherKey, PersistentDataType.INTEGER, 1);
		netherItem.setItemMeta(netherItemMeta);
		netherItem.addUnsafeEnchantment(Enchantment.ARROW_FIRE, 1);
		netherRecipe = new ShapedRecipe(netherKey, netherItem);
		netherRecipe.shape("ABC", "DED", "CBA");
		netherRecipe.setIngredient('A', Material.PHANTOM_MEMBRANE);
		netherRecipe.setIngredient('B', Material.AMETHYST_SHARD);
		netherRecipe.setIngredient('C', Material.ENDER_PEARL);
		netherRecipe.setIngredient('D', Material.ECHO_SHARD);
		netherRecipe.setIngredient('E', Material.TOTEM_OF_UNDYING);
		Bukkit.addRecipe(netherRecipe);

		Bukkit.broadcastMessage("Nether recipe added!");

		NamespacedKey endKey = new NamespacedKey(this, "prEnd");
		ItemStack endItem = new ItemStack(Material.GLOBE_BANNER_PATTERN);
		ItemMeta endItemMeta = endItem.getItemMeta();
		endItemMeta.setDisplayName(ChatColor.GRAY + "Son Parşömeni");
		endItemMeta.setLore(Arrays.asList(ChatColor.GRAY + "Son'un geçidini açan kadim parşömen.", ChatColor.GRAY + "Bu parşömeni okumak yalnızca acı ve keder getirir.", ChatColor.GRAY + "Ne yaptığını bilmeyenler bu parşömene dokunmamalıdır!", ChatColor.GRAY + "Parşömeni okumak için sağ tıkla."));
		endItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		endItemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		endItemMeta.getPersistentDataContainer().set(endKey, PersistentDataType.INTEGER, 1);
		endItem.setItemMeta(endItemMeta);
		endItem.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
		endRecipe = new ShapedRecipe(endKey, endItem);
		endRecipe.shape("ABD", "BCB", "DBA");
		endRecipe.setIngredient('A', Material.WITHER_ROSE);
		endRecipe.setIngredient('B', Material.WITHER_SKELETON_SKULL);
		endRecipe.setIngredient('C', Material.NETHER_STAR);
		endRecipe.setIngredient('D', Material.ENDER_EYE);
		Bukkit.addRecipe(endRecipe);

		Bukkit.broadcastMessage("End recipe added!");
	}

	private void recipeRemover() {
		NamespacedKey netherKey = new NamespacedKey(this, "prNether");
        Bukkit.removeRecipe(netherKey);
        NamespacedKey endKey = new NamespacedKey(this, "prEnd");
        Bukkit.removeRecipe(endKey);
        Bukkit.broadcastMessage("Recipes removed!");
	}

	@EventHandler(ignoreCancelled = true)
	private void onPlayerPortal(PlayerPortalEvent event) {
		if (event.getTo().getWorld().getName().contains("world_nether")) {
			if (event.getPlayer().hasPermission("portalrequirements.nether")) return;
			event.setCancelled(true);
		} else if (event.getTo().getWorld().getName().contains("world_the_end")) {
			if (event.getPlayer().hasPermission("portalrequirements.end")) return;
			Player player = event.getPlayer();
			player.teleport(player.getLocation().add(1, 1, 0));
			event.setCancelled(true);
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
		if (!event.getItem().getType().equals(Material.GLOBE_BANNER_PATTERN)) return;
		Player player = event.getPlayer();
		NamespacedKey netherKey = new NamespacedKey(this, "prNether");
		NamespacedKey endKey = new NamespacedKey(this, "prEnd");
		int nether = event.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(netherKey, PersistentDataType.INTEGER, 0);
		int end = event.getItem().getItemMeta().getPersistentDataContainer().getOrDefault(endKey, PersistentDataType.INTEGER, 0);
		if (nether == 1) {
			if (!player.hasPermission("portalrequirements.nether")) {
				player.getInventory().removeItem(event.getItem());
				getServer().dispatchCommand(getServer().getConsoleSender(), "lp user " + player.getName() + " permission set portalrequirements.nether");
				player.sendMessage(ChatColor.RED + "Cehennem parşömenini okudun ve cehennem azabıyla yüzleşmen gerekiyor!");
				player.sendMessage(ChatColor.GREEN + "Artık Cehennem'in kapıları sana açık, portalları kullanarak oraya ulaşabilirsin.");
				player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 12000, 1));
				player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 12000, 1));
			} else {
				player.sendMessage(ChatColor.RED + "Bu parşömeni zaten okumuşsun!");
			}

		} else if (end == 1) {
			if (!player.hasPermission("portalrequirements.end")) {
				player.getInventory().removeItem(event.getItem());
				getServer().dispatchCommand(getServer().getConsoleSender(), "lp user " + player.getName() + " permission set portalrequirements.end");
				player.sendMessage(ChatColor.GRAY + "Son parşömenini okudun ve dayanılmaz acı ve kederle yüzleşmen gerekiyor!");
				player.sendMessage(ChatColor.GREEN + "Artık Son'a giden yol sana açık, geçitleri kullanarak oraya ulaşabilirsin.");
				player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 12000, 1));
				player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 12000, 1));
			} else {
				player.sendMessage(ChatColor.RED + "Bu parşömeni zaten okumuşsun!");
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




}

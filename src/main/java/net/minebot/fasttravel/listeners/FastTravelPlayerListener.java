/*
 * FastTravelSigns - The Simple Exploration and RPG-Friendly Teleportation Plugin
 *
 * Copyright (c) 2011-2015 craftycreeper, minebot.net, oneill011990
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.minebot.fasttravel.listeners;

import net.minebot.fasttravel.FastTravelSignsPlugin;
import net.minebot.fasttravel.Util.FastTravelUtil;
import net.minebot.fasttravel.data.FastTravelSign;
import net.minebot.fasttravel.data.FastTravelSignDB;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class FastTravelPlayerListener implements Listener {

	private HashMap<UUID, Long> interactLast = new HashMap<UUID, Long>();
    private FastTravelSignsPlugin plugin;

	public FastTravelPlayerListener(FastTravelSignsPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) {

        FastTravelUtil.sendDebug(plugin.getConfig().getBoolean("DevMode"), "Event was called so you made the mistake " +
                "not Bukkit.");

		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		Action action = event.getAction();

		if (!FastTravelUtil.isFTSign(block)
				|| (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK)){
            FastTravelUtil.sendDebug(plugin.getConfig().getBoolean("DevMode"), "You are not clicking a FastTravelSign.");
            return;
        }
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            FastTravelUtil.sendDebug(plugin.getConfig().getBoolean("DevMode"), "You are not clicking it the right way.");
            return;
        }

		Sign sign = (Sign) block.getState();
		String[] lines = sign.getLines();
		String line1 = ChatColor.stripColor(lines[1]);

		FastTravelSign ftsign = FastTravelSignDB.getSign(line1);
		if (ftsign == null) {
            FastTravelUtil.sendDebug(plugin.getConfig().getBoolean("DevMode"), "Couldn't find sign you are clicking.");
            return;
        }

		if (!player.hasPermission("fasttravelsigns.use")) {
            FastTravelUtil.sendDebug(plugin.getConfig().getBoolean("DevMode"), "Could you please stop doing things you" +
                    " are not allowed to do?");
			FastTravelUtil.sendFTMessage(player, "You don't have permission to use fast travel.");
			return;
		}

		long curTime = System.currentTimeMillis() / 1000;
		Long lastTime = interactLast.get(player.getUniqueId());
		if (lastTime != null && curTime - lastTime.longValue() <= 8) {
			// Wait 8 seconds before triggering this again to prevent
			// spamming someone removing a sign
            FastTravelUtil.sendDebug(plugin.getConfig().getBoolean("DevMode"), "Stop clicking me!");
			return;
		}
		interactLast.put(player.getUniqueId(), curTime);
		// Now that the checks are done - see if the user has the sign, and
		// if not, add it.

		if (ftsign.foundBy(player.getUniqueId())) {
			FastTravelUtil.sendFTMessage(player, "You have already added travel point "
					+ ChatColor.AQUA + ftsign.getName() + ChatColor.WHITE + ".");
		} else {
			ftsign.addPlayer(player.getUniqueId());
			FastTravelUtil.sendFTMessage(player,
					"Travel point " + ChatColor.AQUA + ftsign.getName() + ChatColor.WHITE
							+ " added!");
		}

	}

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event){
		if (!plugin.getConfig().getBoolean("use range")){
			return;
		}


        Player p = event.getPlayer();

        if (FastTravelSignDB.getAllSigns() == null){
            return;
        }

        List<FastTravelSign> signs = FastTravelSignDB.getAllSigns();

        if (signs.isEmpty()) {
            return;
        }

        for (FastTravelSign sign : signs){
            if (p.getWorld() != sign.getSignLocation().getWorld()) {
                continue;
            } else if (sign.foundBy(p.getUniqueId())) {
                return;
			} else if (sign.getSignLocation().distance(p.getLocation()) <= sign.getRange()){
                sign.addPlayer(p.getUniqueId());
                FastTravelUtil.sendFTMessage(p, "You have found FastTravel: " + ChatColor.AQUA + sign.getName());
            }
        }

		signs.clear();

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event){
		if (!plugin.getConfig().getBoolean("notify update")){
			return;
		}

        if (event.getPlayer().hasPermission("fasttravelsigns.update") && plugin.needUpdate){
            FastTravelUtil.sendFTMessage(event.getPlayer(), "Update found! You are using " + ChatColor.YELLOW +
                    plugin.getDescription().getVersion() + ChatColor.WHITE + " new version: " + ChatColor.YELLOW +
					plugin.newVersion);
        }
    }

}

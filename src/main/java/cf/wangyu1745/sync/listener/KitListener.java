package cf.wangyu1745.sync.listener;

import cf.wangyu1745.sync.command.Kit;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KitListener implements Listener {

    @SuppressWarnings("unused")
    @EventHandler
    public void invClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        HumanEntity whoClicked = event.getWhoClicked();
        if (clickedInventory.getHolder() instanceof Kit.InvHolder) {
            // 是本插件创建的
            event.setCancelled(true);
        }
    }

}

package cf.wangyu1745.sync.listener;


import org.bukkit.event.Listener;

public class MainListener implements Listener {

    /*@EventHandler
    public void onLogin(PlayerJoinEvent event) {
        try {
            Player p = event.getPlayer();
            double cur = eco.getBalance(p);
            System.out.println("cur = " + cur);
            double dst = Double.parseDouble(new String(jedis.get((PREFIX + p.getName()).getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
            System.out.println("dst = " + dst);
            if (dst > cur) {
                eco.depositPlayer(event.getPlayer(), dst - cur);
            } else {
                eco.withdrawPlayer(event.getPlayer(), cur - dst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        try {
            Player p = event.getPlayer();
            double balance = eco.getBalance(p);
            System.out.println("save " + event.getPlayer().getName() + " " + balance);
            jedis.set((PREFIX + p.getName()).getBytes(StandardCharsets.UTF_8), String.valueOf(balance).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

}

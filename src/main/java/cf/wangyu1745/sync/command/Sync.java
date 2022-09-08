package cf.wangyu1745.sync.command;

import cf.wangyu1745.sync.rpc.Clients;
import cf.wangyu1745.sync.service.IVaultService;
import cf.wangyu1745.sync.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class Sync implements CommandExecutor {
    private final Clients clients;
    private final VaultService vaultService;
//    private final ObjectMapper mapper = new ObjectMapper();
    private final FileConfiguration config;
    private static final String LIST = "list";
    private static final String TRANSFER = "transfer";
    private static final String GIVE = "give";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("sync") && sender instanceof Player) {
            try {
                var p = (Player) sender;
//                List<Clients.ServiceWrapper<Economy>> services = clients.getServices(Economy.class);
                List<Clients.ServiceWrapper<IVaultService>> services = clients.getServices(IVaultService.class);
//                services.add(new Clients.ServiceWrapper<>(config.getString("id"), eco));
                services.add(new Clients.ServiceWrapper<>(config.getString("id"), vaultService));
                switch (args[0]) {
                    case LIST: {
                        CompletableFuture.runAsync(() -> {
//                            services.parallelStream().forEach(economy -> p.sendMessage(economy.getServerId() + ":" + economy.getService().getBalance(p.getName())));
                            services.parallelStream().forEach(vault -> p.sendMessage(vault.getServerId() + ":" + vault.getService().getMoney(p.getName())));
                        });
                        break;
                    }
                    case TRANSFER: {
                        if (args.length != 4) {
                            p.sendMessage("参数错误");
                            p.sendMessage("使用例子: /sync transfer server1 server2 100");
                            p.sendMessage("从server1向server2转100块");
                            return true;
                        }
                        String from = args[1];
                        String to = args[2];
                        double num = Double.parseDouble(args[3]);
                        CompletableFuture.runAsync(() -> {
//                            Optional<Clients.ServiceWrapper<Economy>> from_ = services.stream().filter(e -> e.getServerId().equals(from)).findAny();
                            Optional<Clients.ServiceWrapper<IVaultService>> from_ = services.stream().filter(e -> e.getServerId().equals(from)).findAny();
                            Optional<Clients.ServiceWrapper<IVaultService>> to_ = services.stream().filter(e -> e.getServerId().equals(to)).findAny();
                            if (from_.isPresent() && to_.isPresent()) {
                                var f = from_.get();
                                double balance = f.getService().getMoney(p.getName());
                                if (balance < num) {
                                    p.sendMessage("金额不足");
                                    return;
                                }
                                boolean flag;
                                flag = f.getService().reduceMoney(p.getName(), num);
                                if (!flag) {
                                    p.sendMessage("失败");
                                    return;
                                }
                                var t = to_.get();
                                flag = t.getService().addMoney(p.getName(), num);
                                if (!flag) {
                                    // 把钱退回去，退钱失败了那也没啥好办法。。。
                                    f.getService().addMoney(p.getName(), num);
                                    p.sendMessage("失败");
                                }
                            } else {
                                p.sendMessage("服务器不存在或不在线");
                            }
                        });
                        break;
                    }
                    case GIVE: {
                        if (args.length == 4) {
                            String dstServer = args[1];
                            String dstPlayer = args[2];
                            double num = Double.parseDouble(args[3]);
                            CompletableFuture.runAsync(() -> {
                                Optional<Clients.ServiceWrapper<IVaultService>> a = services.stream().filter(e -> e.getServerId().equals(dstServer)).findAny();
//                                double balance = eco.getBalance(p.getName());
                                double balance = vaultService.getMoney(p.getName());
                                if (!a.isPresent()) {
                                    p.sendMessage("服务器不存在或不在线");
                                    return;
                                }
                                if (balance < num) {
                                    p.sendMessage("余额不足");
                                } else {
//                                    eco.withdrawPlayer(p.getName(), num);
                                    boolean flag;
                                    flag = vaultService.reduceMoney(p.getName(), num);
                                    if (!flag) {
                                        p.sendMessage("失败");
                                        return;
                                    }
                                    flag = a.get().getService().addMoney(dstPlayer, num);
                                    if (!flag) {
                                        // 把钱退回去，退钱失败了那也没啥好办法。。。
                                        vaultService.addMoney(p.getName(), num);
                                        p.sendMessage("失败");
                                    }
                                }
                            });
                        } else {
                            p.sendMessage("参数错误");
                            p.sendMessage("使用例子: /sync give 100 a b");
                            p.sendMessage("向a服的b转账100块");
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}

package cf.wangyu1745.sync.service;

import lombok.RequiredArgsConstructor;
import net.milkbowl.vault.economy.Economy;
import org.springframework.stereotype.Component;

@SuppressWarnings("deprecation")
@Component
@RequiredArgsConstructor
public class VaultService implements IVaultService {
    private final Economy economy;

    @Override
    public boolean addMoney(String name, double money) {
        try {
            return economy.depositPlayer(name, money).transactionSuccess();
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public boolean reduceMoney(String name, double money) {
        try {
            return economy.withdrawPlayer(name, money).transactionSuccess();
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public double getMoney(String name) {
        try {
            return economy.getBalance(name);
        } catch (Exception ignore) {
            return -1;
        }
    }
}

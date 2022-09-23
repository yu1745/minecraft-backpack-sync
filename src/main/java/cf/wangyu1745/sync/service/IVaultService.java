package cf.wangyu1745.sync.service;

public interface IVaultService {
    boolean addMoney(String name, double money);

    boolean reduceMoney(String name, double money);

    double getMoney(String name);
}

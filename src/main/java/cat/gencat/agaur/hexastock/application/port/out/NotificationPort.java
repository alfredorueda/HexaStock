package cat.gencat.agaur.hexastock.application.port.out;

public interface NotificationPort {
    void notifyBuySignal(BuySignal signal);
}

/**
 * Market Data — published primary port.
 *
 * <p>{@link GetStockPriceUseCase} is part of the Market Data module's
 * published API and may be invoked from other Modulith modules (e.g.
 * Portfolio Management). The {@code @NamedInterface} declaration is
 * required because Spring Modulith treats subpackages as internal by
 * default.</p>
 */
@org.springframework.modulith.NamedInterface("port-in")
package cat.gencat.agaur.hexastock.marketdata.application.port.in;

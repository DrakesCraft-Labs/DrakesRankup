package com.drakescraft.rankup;

import com.drakescraft.rankup.model.Rank;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DrakesRankupTest {

    private ServerMock server;
    private DrakesRankupPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DrakesRankupPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testPluginLoadsAndEnables() {
        assertNotNull(plugin);
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getRankManager());
    }

    @Test
    void testFiftyRanksLoaded() {
        assertEquals(50, plugin.getRankManager().getAllRanks().size(), "Deben cargarse exactamente 50 rangos");

        for (int i = 1; i <= 50; i++) {
            Rank rank = plugin.getRankManager().getRankByTier(i);
            assertNotNull(rank, "El rango de tier " + i + " debe existir");
            assertEquals(i, rank.getTier());
            assertNotNull(rank.getDisplayName());
            assertNotNull(rank.getDivision());
            assertTrue(rank.getCost() > 0, "El costo debe ser positivo");
            assertNotNull(rank.getIcon());
        }

        Rank tier1 = plugin.getRankManager().getRankByTier(1);
        assertEquals("primitivo", tier1.getId());
        assertEquals(10000.0, tier1.getCost());

        Rank tier50 = plugin.getRankManager().getRankByTier(50);
        assertEquals("rey_de_los_piratas", tier50.getId());
        assertEquals(1500000000.0, tier50.getCost());
    }

    @Test
    void testPlayerProgressionCalculation() {
        PlayerMock player = server.addPlayer("Goku");
        
        assertEquals(0, plugin.getRankManager().getPlayerTier(player.getUniqueId()));
        assertNull(plugin.getRankManager().getPlayerRank(player.getUniqueId()));

        Rank next = plugin.getRankManager().getNextRank(player.getUniqueId());
        assertNotNull(next);
        assertEquals(1, next.getTier());
        assertEquals("primitivo", next.getId());

        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 25);
        assertEquals(25, plugin.getRankManager().getPlayerTier(player.getUniqueId()));
        assertEquals("supernova", plugin.getRankManager().getPlayerRank(player.getUniqueId()).getId());

        Rank nextAfter25 = plugin.getRankManager().getNextRank(player.getUniqueId());
        assertNotNull(nextAfter25);
        assertEquals(26, nextAfter25.getTier());

        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 50);
        assertNull(plugin.getRankManager().getNextRank(player.getUniqueId()), "En tier 50 no debe haber siguiente rango");
    }
}

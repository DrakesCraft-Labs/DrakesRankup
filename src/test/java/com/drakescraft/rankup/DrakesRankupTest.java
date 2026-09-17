package com.drakescraft.rankup;

import com.drakescraft.rankup.gui.TransformationMenu;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
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
        assertNotNull(plugin.getStaffManager());
        assertNotNull(plugin.getDragonBallListener());
        assertNotNull(plugin.getOnePieceListener());
    }

    @Test
    void testFiftyAnimeRanksLoaded() {
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
        assertEquals("senku", tier1.getId());
        assertEquals(10000.0, tier1.getCost());
        assertEquals(AbilityType.DOUBLE_DROP, tier1.getAbilityType());
        assertFalse(tier1.isHasKineticPush());

        Rank tier11 = plugin.getRankManager().getRankByTier(11);
        assertEquals("genin", tier11.getId());
        assertTrue(tier11.isHasKineticPush());

        Rank tier18 = plugin.getRankManager().getRankByTier(18);
        assertEquals("yuji", tier18.getId());
        assertEquals(AbilityType.BLACK_FLASH, tier18.getAbilityType());

        Rank tier19 = plugin.getRankManager().getRankByTier(19);
        assertEquals("gojo", tier19.getId());
        assertEquals(AbilityType.MUGEN_DEFENSE, tier19.getAbilityType());

        Rank tier24 = plugin.getRankManager().getRankByTier(24);
        assertEquals("shichibukai", tier24.getId());
        assertEquals(AbilityType.SANTORYU_ZORO, tier24.getAbilityType());

        Rank tier30 = plugin.getRankManager().getRankByTier(30);
        assertEquals("joyboy", tier30.getId());
        assertEquals(AbilityType.DEVIL_FRUIT_GOMU, tier30.getAbilityType());

        Rank tier34 = plugin.getRankManager().getRankByTier(34);
        assertEquals("ssj3", tier34.getId());
        assertEquals(AbilityType.BROLY_LSSJ, tier34.getAbilityType());

        Rank tier35 = plugin.getRankManager().getRankByTier(35);
        assertEquals("ssjgod", tier35.getId());
        assertEquals(AbilityType.SSJ_GOD, tier35.getAbilityType());

        Rank tier36 = plugin.getRankManager().getRankByTier(36);
        assertEquals("ssjblue", tier36.getId());
        assertEquals(AbilityType.SSJ_BLUE, tier36.getAbilityType());

        Rank tier45 = plugin.getRankManager().getRankByTier(45);
        assertEquals("hakari", tier45.getId());
        assertEquals(AbilityType.GOHAN_BEAST, tier45.getAbilityType());

        Rank tier46 = plugin.getRankManager().getRankByTier(46);
        assertEquals("ultrainstinto", tier46.getId());
        assertEquals(AbilityType.MASTERED_ULTRA_INSTINCT, tier46.getAbilityType());

        Rank tier47 = plugin.getRankManager().getRankByTier(47);
        assertEquals("beerus", tier47.getId());
        assertEquals(AbilityType.ULTRA_EGO, tier47.getAbilityType());

        Rank tier48 = plugin.getRankManager().getRankByTier(48);
        assertEquals("whis", tier48.getId());
        assertEquals(AbilityType.VEGETTO_SPIRIT_SWORD, tier48.getAbilityType());

        Rank tier50 = plugin.getRankManager().getRankByTier(50);
        assertEquals("kamisama", tier50.getId());
        assertEquals(1500000000.0, tier50.getCost());
        assertEquals(AbilityType.KAMI_DIVINE, tier50.getAbilityType());
        assertTrue(tier50.isHasKineticPush());
    }

    @Test
    void testPlayerProgressionCalculation() {
        PlayerMock player = server.addPlayer("Goku");
        
        assertEquals(0, plugin.getRankManager().getPlayerTier(player.getUniqueId()));
        assertNull(plugin.getRankManager().getPlayerRank(player.getUniqueId()));

        Rank next = plugin.getRankManager().getNextRank(player.getUniqueId());
        assertNotNull(next);
        assertEquals(1, next.getTier());
        assertEquals("senku", next.getId());

        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 25);
        assertEquals(25, plugin.getRankManager().getPlayerTier(player.getUniqueId()));
        assertEquals("yonkou", plugin.getRankManager().getPlayerRank(player.getUniqueId()).getId());

        Rank nextAfter25 = plugin.getRankManager().getNextRank(player.getUniqueId());
        assertNotNull(nextAfter25);
        assertEquals(26, nextAfter25.getTier());
        assertEquals("shinigami", nextAfter25.getId());

        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 50);
        assertNull(plugin.getRankManager().getNextRank(player.getUniqueId()), "En tier 50 no debe haber siguiente rango");
    }

    @Test
    void testStaffAngelAndTestMode() {
        PlayerMock staff = server.addPlayer("WhisStaff");

        // Test Angel mode toggle
        assertFalse(plugin.getStaffManager().isAngel(staff.getUniqueId()));
        assertTrue(plugin.getStaffManager().toggleAngel(staff));
        assertTrue(plugin.getStaffManager().isAngel(staff.getUniqueId()));
        assertTrue(staff.isInvulnerable());

        assertFalse(plugin.getStaffManager().toggleAngel(staff));
        assertFalse(plugin.getStaffManager().isAngel(staff.getUniqueId()));
        assertFalse(staff.isInvulnerable());

        // Test Rank Test mode
        assertEquals(0, plugin.getRankManager().getPlayerTier(staff.getUniqueId()));
        assertTrue(plugin.getStaffManager().setTestTier(staff, 46));
        assertEquals(46, plugin.getRankManager().getPlayerTier(staff.getUniqueId()));

        assertTrue(plugin.getStaffManager().resetTestTier(staff));
        assertEquals(0, plugin.getRankManager().getPlayerTier(staff.getUniqueId()));
    }

    @Test
    void testTransformationSelectionAndMenu() {
        PlayerMock player = server.addPlayer("Vegeta");
        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 47);

        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        assertNull(settings.getActiveTransformation());
        assertTrue(settings.isKiFlightEnabled());

        settings.setActiveTransformation("ULTRA_EGO");
        assertEquals("ULTRA_EGO", settings.getActiveTransformation());

        TransformationMenu menu = new TransformationMenu(plugin, player);
        assertDoesNotThrow(menu::open);
        assertNotNull(player.getOpenInventory().getTopInventory());
        assertEquals(54, player.getOpenInventory().getTopInventory().getSize());
    }
}

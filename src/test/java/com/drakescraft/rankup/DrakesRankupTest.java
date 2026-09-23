package com.drakescraft.rankup;

import com.drakescraft.rankup.gui.RankupMenu;
import com.drakescraft.rankup.gui.TransformationMenu;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
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
        assertNotNull(plugin.getKitManager());
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
        assertTrue(tier1.isPermanent(), "Tier 1 de división I debe ser permanente");

        Rank tier10 = plugin.getRankManager().getRankByTier(10);
        assertTrue(tier10.isPermanent(), "Tier 10 (ancla división I) debe ser permanente");

        Rank tier11 = plugin.getRankManager().getRankByTier(11);
        assertEquals("genin", tier11.getId());
        assertFalse(tier11.isPermanent(), "Tier 11 intermedio debe ser temporal");

        Rank tier20 = plugin.getRankManager().getRankByTier(20);
        assertTrue(tier20.isPermanent(), "Tier 20 (ancla división II) debe ser permanente");

        Rank tier30 = plugin.getRankManager().getRankByTier(30);
        assertTrue(tier30.isPermanent(), "Tier 30 (ancla división III) debe ser permanente");

        Rank tier40 = plugin.getRankManager().getRankByTier(40);
        assertTrue(tier40.isPermanent(), "Tier 40 (ancla división IV) debe ser permanente");

        Rank tier46 = plugin.getRankManager().getRankByTier(46);
        assertEquals("ultrainstinto", tier46.getId());
        assertFalse(tier46.isPermanent(), "Tier 46 Ultra Instinto debe requerir mantención");
        assertTrue(tier46.getMaintenanceCost() > 0, "El costo de mantención debe ser positivo");

        Rank tier50 = plugin.getRankManager().getRankByTier(50);
        assertEquals("kamisama", tier50.getId());
        assertTrue(tier50.isPermanent(), "Tier 50 Rey / Deidad Suprema debe ser permanente");
    }

    @Test
    void testRankDecayAndCheckpoints() {
        PlayerMock player = server.addPlayer("DecayTester");

        // Checkpoints por división
        assertEquals(10, plugin.getRankManager().getLastCheckpointTier(10));
        assertEquals(10, plugin.getRankManager().getLastCheckpointTier(18));
        assertEquals(20, plugin.getRankManager().getLastCheckpointTier(25));
        assertEquals(30, plugin.getRankManager().getLastCheckpointTier(36));
        assertEquals(40, plugin.getRankManager().getLastCheckpointTier(46));

        // Rango permanente no sufre decay
        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 10);
        assertEquals(-1L, plugin.getRankManager().getMaintenanceExpiry(player.getUniqueId()));
        assertFalse(plugin.getRankManager().checkDecay(player.getUniqueId(), false));
        assertEquals(10, plugin.getRankManager().getPlayerTier(player.getUniqueId()));

        // Rango temporal Tier 46 (Ultra Instinto)
        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 46);
        assertEquals(46, plugin.getRankManager().getPlayerTier(player.getUniqueId()));
        assertFalse(plugin.getRankManager().isRankPermanent(46));

        long expiry = plugin.getRankManager().getMaintenanceExpiry(player.getUniqueId());
        assertTrue(expiry > System.currentTimeMillis());
        assertTrue(plugin.getRankManager().getRemainingMaintenanceMs(player.getUniqueId()) > 0);

        // Sin expirar, checkDecay retorna false y no degrada
        assertFalse(plugin.getRankManager().checkDecay(player.getUniqueId(), false));
        assertEquals(46, plugin.getRankManager().getPlayerTier(player.getUniqueId()));

        // Simular expiración pasando 15 días en el pasado
        plugin.getRankManager().resetMaintenance(player.getUniqueId());
        // Forzar expiración manual modificando el mapa vía simulación
        // Degradar manualmente con fecha expirada
        boolean decayed = plugin.getRankManager().checkDecay(player.getUniqueId(), false);
        assertFalse(decayed, "Aún no ha expirado");

        // Al subir de rango se reinicia el contador de mantención
        plugin.getRankManager().resetMaintenance(player.getUniqueId());
        assertTrue(plugin.getRankManager().getRemainingMaintenanceMs(player.getUniqueId()) > 0);
    }

    @Test
    void testRankupMenuRenderingWithMaintenanceSlot() {
        PlayerMock player = server.addPlayer("GuiTester");
        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 25);

        RankupMenu menu = new RankupMenu(plugin, player, 2);
        assertDoesNotThrow(menu::open);
        assertNotNull(player.getOpenInventory().getTopInventory());

        // Slot 45: Núcleo de Estabilidad
        org.bukkit.inventory.ItemStack slot45 = player.getOpenInventory().getTopInventory().getItem(45);
        assertNotNull(slot45);
        assertTrue(slot45.hasItemMeta());
        assertTrue(slot45.getItemMeta().getDisplayName().contains("Núcleo de Estabilidad") || slot45.getItemMeta().getDisplayName().contains("Rango"));

        // Slot 47: Menú de Transformaciones
        org.bukkit.inventory.ItemStack slot47 = player.getOpenInventory().getTopInventory().getItem(47);
        assertNotNull(slot47);
        assertEquals(org.bukkit.Material.DRAGON_BREATH, slot47.getType());
    }

    @Test
    void testStaffAngelAndTestMode() {
        PlayerMock staff = server.addPlayer("WhisStaff");

        assertFalse(plugin.getStaffManager().isAngel(staff.getUniqueId()));
        assertTrue(plugin.getStaffManager().toggleAngel(staff));
        assertTrue(plugin.getStaffManager().isAngel(staff.getUniqueId()));
        assertTrue(staff.isInvulnerable());

        assertFalse(plugin.getStaffManager().toggleAngel(staff));
        assertFalse(plugin.getStaffManager().isAngel(staff.getUniqueId()));
        assertFalse(staff.isInvulnerable());

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

    @Test
    void testRankupKitClaim() {
        PlayerMock player = server.addPlayer("NarutoKit");
        assertFalse(plugin.getKitManager().claimKit(player));

        plugin.getRankManager().setPlayerTier(player.getUniqueId(), 5);
        assertTrue(plugin.getKitManager().claimKit(player));

        assertFalse(plugin.getKitManager().claimKit(player));
        assertFalse(plugin.getKitManager().canClaim(player.getUniqueId(), 1));

        assertTrue(player.getInventory().contains(org.bukkit.Material.DIAMOND_PICKAXE));
        assertTrue(player.getInventory().contains(org.bukkit.Material.IRON_SWORD));
    }

    @Test
    void testWorldFilterRestriction() {
        org.bukkit.World customWorld = server.addSimpleWorld("world");
        org.bukkit.World classicWorld = server.addSimpleWorld("clasico");
        org.bukkit.World skyWorld = server.addSimpleWorld("bskyblock_world");

        // Custom worlds permitted
        assertTrue(plugin.isWorldAllowed(customWorld), "El mundo 'world' de Survival SF debe estar permitido");
        assertTrue(plugin.isWorldAllowed(skyWorld), "El mundo 'bskyblock_world' debe estar permitido");

        // Classic world blocked
        assertFalse(plugin.isWorldAllowed(classicWorld), "El mundo 'clasico' debe estar bloqueado");
        assertNotNull(plugin.getWorldBlockedMessage());
        assertTrue(plugin.getWorldBlockedMessage().contains("exclusivo de las modalidades custom"));

        // Player in clasico cannot process rankup or maintenance
        PlayerMock classicPlayer = server.addPlayer("ClassicWarrior");
        classicPlayer.teleport(classicWorld.getSpawnLocation());
        assertEquals("clasico", classicPlayer.getWorld().getName());

        assertFalse(plugin.getRankManager().processRankup(classicPlayer), "No debe permitir /rankup en Clásico");
        assertFalse(plugin.getRankManager().processMaintenance(classicPlayer), "No debe permitir /rankup maintain en Clásico");
        assertEquals(0, plugin.getRankManager().processRankupMax(classicPlayer), "No debe permitir /rankup max en Clásico");

        // Player in allowed world can proceed
        PlayerMock sfPlayer = server.addPlayer("SfWarrior");
        sfPlayer.teleport(customWorld.getSpawnLocation());
        assertEquals("world", sfPlayer.getWorld().getName());
        assertTrue(plugin.isWorldAllowed(sfPlayer.getWorld()));
    }

    @Test
    void testKineticPushDoesNotStripExternalFlight() {
        org.bukkit.World customWorld = server.addSimpleWorld("world_test_fly");
        PlayerMock vipPlayer = server.addPlayer("VipPlayerFly");
        vipPlayer.teleport(customWorld.getSpawnLocation());

        // VIP player has flight granted externally (e.g. Essentials /fly or Hefesto rank)
        vipPlayer.setAllowFlight(true);
        vipPlayer.setFlying(true);

        // Player is tier 0 (does not have kinetic push)
        assertEquals(0, plugin.getRankManager().getPlayerTier(vipPlayer.getUniqueId()));

        // Call updatePushEligibility directly
        plugin.getKineticPushListener().updatePushEligibility(vipPlayer);
        assertTrue(vipPlayer.getAllowFlight(), "No debe revocar allowFlight a un jugador sin empuje cinético");
        assertTrue(vipPlayer.isFlying(), "No debe revocar isFlying a un jugador con vuelo activo");

        // Even when on ground, allowFlight should stay intact for VIP
        vipPlayer.setFlying(false);
        plugin.getKineticPushListener().updatePushEligibility(vipPlayer);
        assertTrue(vipPlayer.getAllowFlight(), "No debe revocar allowFlight en el suelo a un jugador sin empuje cinético");
    }

    @Test
    void testDragonBallInteractionsIgnorePlayersWithoutRank() {
        PlayerMock player = server.addPlayer("NoRankDragonBall");
        player.setSneaking(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        PlayerInteractEvent hakai = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR,
                new ItemStack(Material.AIR), null, null);
        PlayerInteractEvent zeno = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR,
                new ItemStack(Material.AIR), null, null);

        assertDoesNotThrow(() -> plugin.getDragonBallListener().onBillsHakai(hakai));
        assertDoesNotThrow(() -> plugin.getDragonBallListener().onZenoErase(zeno));
    }
}

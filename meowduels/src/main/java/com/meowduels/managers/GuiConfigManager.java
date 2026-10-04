package com.meowduels.managers;

import com.meowduels.MeowDuels;
import com.meowduels.util.Items;
import com.meowduels.util.Tokens;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * The menus, as files: one {@code gui/<menu>.yml} per screen, holding its
 * title, its height, and every item's material, name, lore, stack size and
 * slot.
 *
 * <p>Shaped around one rule, which is the only reason a change this wide is
 * safe on a build that cannot be tested in game: <b>every default is the
 * literal that is in the code.</b> Each lookup below takes the value the menu
 * would have used anyway and returns it unless a file says otherwise. Delete
 * the {@code gui/} folder, or one file in it, or one key inside one file, and
 * the screen renders exactly as it did before any of this existed.
 *
 * <p>That is why nothing here reads a default out of a resource and why there
 * is no "reset to defaults" command: deleting is the reset.
 *
 * <h2>Why the lookups are per item rather than per menu</h2>
 *
 * <p>The obvious design - hand this class a menu and let it paint the whole
 * inventory - cannot work here. Nine menus override {@code open(Player)} with
 * party and leader guards that return before anything is built, and
 * {@code QueuePickMenu} registers itself in a static set as it opens. A
 * painter hooked on {@code build()} would miss all of that. So the menus keep
 * their own control flow and call in at each item instead.
 *
 * <h2>Slots are not editable everywhere</h2>
 *
 * <p>Five menus - {@code ArenaMenu}, {@code GuiEditorMenu}, {@code TrimMenu},
 * {@code KitSelectMenu} and {@code KitItemsEditMenu} - route clicks on
 * {@code event.getRawSlot()} against hardcoded numbers rather than on the
 * item's tag, so moving an item would fire the wrong handler. Their files ship
 * without a {@code slot:} key and {@link #slot} refuses to read one for them.
 * A key that silently does nothing is worse than no key, so this fails loudly
 * in the checker instead of quietly on the server.
 */
public class GuiConfigManager {
    /**
     * Menus whose clicks are dispatched by slot number, not by tag.
     *
     * <p>Kept here rather than as a flag on each menu because
     * {@code tools/verify_gui.py} reads this list to check that no shipped
     * file carries a {@code slot:} key for one of them.
     */
    private static final List<String> FIXED_SLOTS = Arrays.asList(
            "arena", "guieditor", "trim", "kitselect", "kititems");

    private final MeowDuels plugin;
    private final File folder;
    private final Map<String, YamlConfiguration> files = new LinkedHashMap<String, YamlConfiguration>();

    public GuiConfigManager(MeowDuels plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "gui");
        this.reload();
    }

    /** True for a menu that cannot have its slots moved - see the class note. */
    public static boolean slotsAreFixed(String menuId) {
        return FIXED_SLOTS.contains(menuId);
    }

    /**
     * Re-reads every {@code gui/*.yml}, writing out the shipped copies first
     * if the folder is not there yet.
     *
     * <p>A file that fails to parse is dropped with a warning rather than
     * taken half-loaded: a menu with no file falls back to the code, which is
     * a working screen, while a menu with half a file is not.
     */
    public void reload() {
        this.files.clear();
        this.copyShipped();
        File[] found = this.folder.listFiles();
        if (found == null) {
            return;
        }
        for (File file : found) {
            String name = file.getName();
            if (!name.endsWith(".yml")) continue;
            String id = name.substring(0, name.length() - 4).toLowerCase();
            try {
                this.files.put(id, YamlConfiguration.loadConfiguration(file));
            }
            catch (Throwable t) {
                this.plugin.getLogger().warning("gui/" + name + " could not be read, so the "
                        + id + " menu will use its built-in layout: " + t.getMessage());
            }
        }
    }

    /**
     * Puts the shipped menu files in place on a server that has none.
     *
     * <p>Only ever writes a file that is absent. An owner who has edited
     * {@code gui/party.yml} and then updates the jar keeps their edits; a key
     * this build added that their file lacks falls back to the code default,
     * which is the same value the shipped file would have given them.
     */
    private void copyShipped() {
        for (String id : MENUS) {
            File file = new File(this.folder, id + ".yml");
            if (file.exists()) continue;
            try {
                this.plugin.saveResource("gui/" + id + ".yml", false);
            }
            catch (Throwable t) {
                // A menu with no file uses the code defaults, so there is
                // nothing to recover from here and nothing worth a warning on
                // every boot.
            }
        }
    }

    /**
     * Floors a window with its filler pane.
     *
     * <p>A menu file may set {@code filler:} and {@code filler-name:} to
     * break from the house frame; with neither, this is exactly
     * {@code Style.frame} and the theme's pane is used.
     */
    public void frame(String menuId, org.bukkit.inventory.Inventory inventory, int rows) {
        YamlConfiguration file = this.files.get(menuId);
        if (file == null || !file.contains("filler")) {
            com.meowduels.gui.Style.frame(inventory, rows);
            return;
        }
        com.meowduels.gui.Style.frame(inventory, rows, com.meowduels.gui.Style.filler(
                file.getString("filler"),
                file.getString("filler-name", com.meowduels.gui.Style.FILLER_NAME)));
    }

    private ConfigurationSection itemSection(String menuId, String itemId) {
        YamlConfiguration file = this.files.get(menuId);
        return file == null ? null : file.getConfigurationSection("items." + itemId);
    }

    /** The window title, or {@code def} if no file overrides it. */
    public String title(String menuId, String def) {
        YamlConfiguration file = this.files.get(menuId);
        if (file == null) {
            return def;
        }
        String value = file.getString("title");
        return value == null ? def : value;
    }

    /** The window title with {@code {token}}s filled in. */
    public String title(String menuId, String def, Map<String, String> tokens) {
        return Tokens.apply(this.title(menuId, def), tokens);
    }

    /**
     * One of a screen's several titles, as {@code title-<variant>}.
     *
     * <p>Four menus are opened in two modes and name themselves differently
     * in each - the kit editor is "Your Kits" to a player and "Kit Editor" to
     * an admin, and the kit picker and queue grow a category name once you
     * page into one. Falling back to the plain {@code title} keeps a file
     * that only sets that one working for both.
     */
    public String titleOf(String menuId, String variant, String def, Map<String, String> tokens) {
        YamlConfiguration file = this.files.get(menuId);
        if (file == null) {
            return Tokens.apply(def, tokens);
        }
        String value = file.getString("title-" + variant, file.getString("title", def));
        return Tokens.apply(value, tokens);
    }

    /**
     * The window height in rows, clamped to what a chest can actually be.
     *
     * <p>A chest inventory outside 1-6 rows throws on creation, which would
     * take the menu down rather than look wrong, so a nonsense value in a file
     * is corrected and reported instead of passed through.
     */
    public int rows(String menuId, int def) {
        YamlConfiguration file = this.files.get(menuId);
        if (file == null) {
            return def;
        }
        int value = file.getInt("rows", def);
        if (value >= 1 && value <= 6) {
            return value;
        }
        this.plugin.getLogger().warning("gui/" + menuId + ".yml: rows is " + value
                + ", which is not between 1 and 6 - using " + def + ".");
        return def;
    }

    /**
     * Where an item sits, or {@code def} where the menu will not take a move.
     *
     * <p>The slot is also range-checked against the window: a slot past the
     * end would silently drop the item, which on a confirm screen means a
     * button nobody can press.
     */
    public int slot(String menuId, String itemId, int def, int rows) {
        if (GuiConfigManager.slotsAreFixed(menuId)) {
            return def;
        }
        ConfigurationSection section = this.itemSection(menuId, itemId);
        if (section == null || !section.contains("slot")) {
            return def;
        }
        int value = section.getInt("slot", def);
        if (value >= 0 && value < rows * 9) {
            return value;
        }
        this.plugin.getLogger().warning("gui/" + menuId + ".yml: " + itemId + " has slot "
                + value + ", which is outside this " + rows + "-row menu - using " + def + ".");
        return def;
    }

    /**
     * An item, built from the file where it says anything and from the passed
     * defaults everywhere else.
     *
     * <p>Returns the builder rather than a finished stack so each call site
     * keeps the {@code .tag(...)}, {@code .skull(...)} and
     * {@code .hideTooltip()} it already had.
     *
     * <p>Config text goes in through {@code rawName}/{@code rawLore}, never
     * {@code name}/{@code lore}: the latter pair re-cases every letter to
     * small caps and mangles a MiniMessage tag into literal text. Menus that
     * were written with {@code name}/{@code lore} have their defaults passed
     * in already small-capped, so what comes out the other side is unchanged.
     */
    public Items item(String menuId, String itemId, Material defMaterial, String defName, String... defLore) {
        return this.item(menuId, itemId, null, defMaterial, defName, defLore);
    }

    /** As {@link #item}, with {@code {token}}s filled in on the name and lore. */
    public Items item(String menuId, String itemId, Map<String, String> tokens,
                      Material defMaterial, String defName, String... defLore) {
        ConfigurationSection section = this.itemSection(menuId, itemId);
        Material material = this.material(menuId, itemId, section, defMaterial);
        int amount = section == null ? 1 : Math.max(1, Math.min(64, section.getInt("amount", 1)));
        String name = section == null ? defName : section.getString("name", defName);
        List<String> lore = this.lore(section, defLore);
        Items items = Items.of(material, amount).rawName(Tokens.apply(name, tokens));
        List<String> filled = new ArrayList<String>(lore.size());
        for (String line : lore) {
            filled.add(Tokens.apply(line, tokens));
        }
        items.rawLore(filled.toArray(new String[0]));
        if (section != null && section.contains("glow")) {
            items.glint(Boolean.valueOf(section.getBoolean("glow")));
        }
        return items;
    }

    /**
     * As {@link #item}, for a button that reads one way when a thing is on and
     * another when it is off.
     *
     * <p>Most of the variable text in these menus is a state switch rather
     * than a substitution - "Enabled" against "Disabled", a lime dye against a
     * grey one - and a single {@code name:} with a token in it cannot express
     * that. This reads {@code name-on}/{@code name-off} and
     * {@code material-on}/{@code material-off}, each falling back to the plain
     * {@code name}/{@code material} and then to the code.
     */
    public Items toggle(String menuId, String itemId, boolean on, Map<String, String> tokens,
                        Face faceOn, Face faceOff) {
        Face face = on ? faceOn : faceOff;
        String suffix = on ? "-on" : "-off";
        ConfigurationSection section = this.itemSection(menuId, itemId);
        Material material = face.material;
        String name = face.name;
        List<String> lore = Arrays.asList(face.lore);
        Boolean glow = face.glow ? Boolean.TRUE : null;
        if (section != null) {
            String named = section.contains("material" + suffix)
                    ? section.getString("material" + suffix)
                    : section.getString("material");
            material = this.material(menuId, itemId, named, material);
            name = section.getString("name" + suffix, section.getString("name", name));
            if (section.contains("lore" + suffix)) {
                lore = section.getStringList("lore" + suffix);
            } else if (section.contains("lore")) {
                lore = section.getStringList("lore");
            }
            if (section.contains("glow" + suffix)) {
                glow = Boolean.valueOf(section.getBoolean("glow" + suffix));
            } else if (section.contains("glow")) {
                glow = Boolean.valueOf(section.getBoolean("glow"));
            }
        }
        return this.assemble(material, this.amount(menuId, itemId, 1), name, lore, glow, tokens);
    }

    /** One item, where the code has a single appearance for it. */
    public Items face(String menuId, String itemId, Map<String, String> tokens, Face face) {
        ConfigurationSection section = this.itemSection(menuId, itemId);
        Material material = this.material(menuId, itemId, section, face.material);
        String name = section == null ? face.name : section.getString("name", face.name);
        List<String> lore = this.lore(section, face.lore);
        Boolean glow = face.glow ? Boolean.TRUE : null;
        if (section != null && section.contains("glow")) {
            glow = Boolean.valueOf(section.getBoolean("glow"));
        }
        return this.assemble(material, this.amount(menuId, itemId, 1), name, lore, glow, tokens);
    }

    private Items assemble(Material material, int amount, String name, List<String> lore,
                           Boolean glow, Map<String, String> tokens) {
        Items items = Items.of(material, amount).rawName(Tokens.apply(name, tokens));
        List<String> filled = new ArrayList<String>(lore.size());
        for (String line : lore) {
            filled.add(Tokens.apply(line, tokens));
        }
        items.rawLore(filled.toArray(new String[0]));
        return items.glint(glow);
    }

    /**
     * How one item looks in one state.
     *
     * <p>Several buttons here read differently depending on what they
     * control - a map is selected, compatible, or incompatible; an effect is
     * on or off - and each state can want its own icon, wording and shimmer.
     * Passing those as a growing list of parallel arguments got unreadable at
     * about six; naming the pair of states is clearer, and the checker can
     * still read every literal out of it.
     */
    public static final class Face {
        private final Material material;
        private final String name;
        private final String[] lore;
        private boolean glow;

        private Face(Material material, String name, String[] lore) {
            this.material = material;
            this.name = name;
            this.lore = lore;
        }

        public static Face of(Material material, String name, String... lore) {
            return new Face(material, name, lore);
        }

        /** Marks this state as the one that shimmers. */
        public Face glow() {
            this.glow = true;
            return this;
        }
    }

    /**
     * A name override, or {@code null} where the file gives none.
     *
     * <p>For the list screens, where an entry's label is not a constant the
     * code could hand over as a default. A kit row is titled by
     * {@code miniName(kit.getDisplayName())} when the kit has a display name
     * and by a small-capped {@code &e<id>} when it does not - two different
     * renderers, chosen per kit at runtime.
     *
     * <p>Collapsing that into one template would quietly restyle every kit
     * on the server, so the Java keeps deciding and this only speaks up when
     * an owner has actually written a {@code name:} for the row.
     */
    public String name(String menuId, String itemId, Map<String, String> tokens) {
        ConfigurationSection section = this.itemSection(menuId, itemId);
        if (section == null) {
            return null;
        }
        return Tokens.apply(section.getString("name"), tokens);
    }

    /** Lore for an entry whose other properties the code decides. */
    public List<String> lore(String menuId, String itemId, Map<String, String> tokens, String... defLore) {
        List<String> lore = this.lore(this.itemSection(menuId, itemId), defLore);
        List<String> filled = new ArrayList<String>(lore.size());
        for (String line : lore) {
            filled.add(Tokens.apply(line, tokens));
        }
        return filled;
    }

    /**
     * A material override, defaulting to one the code worked out.
     *
     * <p>Separate from {@link #item} because a kit row's icon comes from the
     * kit, so there is no literal to ship as the default - the shipped file
     * leaves {@code material:} out entirely and setting it pins every row to
     * the same icon, which is a thing an owner may well want and should not
     * get by accident.
     */
    public Material material(String menuId, String itemId, Material def) {
        return this.material(menuId, itemId, this.itemSection(menuId, itemId), def);
    }

    /** The stack size for an entry, where the code has no opinion. */
    public int amount(String menuId, String itemId, int def) {
        ConfigurationSection section = this.itemSection(menuId, itemId);
        if (section == null || !section.contains("amount")) {
            return def;
        }
        return Math.max(1, Math.min(64, section.getInt("amount", def)));
    }

    private List<String> lore(ConfigurationSection section, String[] defLore) {
        if (section == null || !section.contains("lore")) {
            return Arrays.asList(defLore);
        }
        return section.getStringList("lore");
    }

    private Material material(String menuId, String itemId, ConfigurationSection section, Material def) {
        return section == null ? def : this.material(menuId, itemId, section.getString("material"), def);
    }

    /**
     * A material by name, falling back to the code where the name is wrong.
     *
     * <p>Named rather than numeric, and reported when it does not resolve: a
     * typo in a material is the single likeliest mistake in a hand-edited menu
     * file, and the symptom without this - one item quietly missing from a
     * screen - gives an owner nothing to go on.
     */
    private Material material(String menuId, String itemId, String name, Material def) {
        if (name == null || name.isEmpty()) {
            return def;
        }
        Material found = Material.matchMaterial(name);
        if (found != null && found.isItem()) {
            return found;
        }
        this.plugin.getLogger().warning("gui/" + menuId + ".yml: " + itemId + " has material '"
                + name + "', which is not an item - using " + def + ".");
        return def;
    }

    /**
     * Every menu with a shipped file, which is every menu.
     *
     * <p>Also read by {@code tools/verify_gui.py}, which checks each id here
     * has a resource, that the resource's values match the defaults in the
     * Java, and that none of the fixed-slot menus ships a {@code slot:}.
     */
    public static final List<String> MENUS = Arrays.asList(
            "arena", "duelconfirm", "editkiteffects", "editkitlist", "eventmap",
            "guieditor", "kiteditor", "kititems", "kitpick", "kitselect",
            "mapselect", "matchsummary", "partyconfirm", "partyduelsconfirm",
            "partyinvite", "partykit", "party", "partymode", "partyopponent",
            "partysettings", "partystartconfirm", "partyteam", "queuepick",
            "settings", "tabconfig", "trimkit", "trim");
}

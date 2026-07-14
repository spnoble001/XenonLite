package xenon.dev.modules.mods.combat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.network.NetworkPlayerInfo;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;

/**
 * Lista local de jugadores que AimAssist debe tratar como integrantes del equipo.
 * Los IGN se resuelven localmente contra la lista de jugadores de la pestaña Tab.
 */
@Mod(keybind = 0)
public final class Equipo extends Module {
    private final Set<UUID> jugadores = new HashSet<UUID>();
    private final Map<UUID, String> nombres = new LinkedHashMap<UUID, String>();

    public Equipo() {
        this.name = "Equipo";
    }

    @Override
    public char getSort() {
        return 'e';
    }

    public boolean contiene(UUID uuid) {
        return uuid != null && this.jugadores.contains(uuid);
    }

    public boolean agregar(String ign) {
        if (ign == null || ign.trim().isEmpty() || this.minecraft.getNetHandler() == null) {
            return false;
        }
        NetworkPlayerInfo info = this.minecraft.getNetHandler().getPlayerInfo(ign.trim());
        if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null) {
            return false;
        }
        UUID uuid = info.getGameProfile().getId();
        boolean agregado = this.jugadores.add(uuid);
        this.nombres.put(uuid, info.getGameProfile().getName());
        return agregado;
    }

    public boolean eliminar(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        this.nombres.remove(uuid);
        return this.jugadores.remove(uuid);
    }

    public Set<UUID> getJugadores() {
        return Collections.unmodifiableSet(this.jugadores);
    }

    public Map<UUID, String> getNombres() {
        return Collections.unmodifiableMap(new LinkedHashMap<UUID, String>(this.nombres));
    }
}

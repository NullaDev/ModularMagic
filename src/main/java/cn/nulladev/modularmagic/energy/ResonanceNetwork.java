package cn.nulladev.modularmagic.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.HashSet;
import java.util.UUID;

/** Explicit single-parent forest. All traversal uses loaded block entities, never chunk loading. */
public final class ResonanceNetwork {
    public static boolean isParent(EnergyBlockEntity source,EnergyBlockEntity child) {
        return source.getBlockPos().equals(child.resonanceParent)&&source.identity.equals(child.resonanceParentIdentity);
    }
    public static boolean inRange(EnergyBlockEntity source,EnergyBlockEntity child) {
        return source.resonanceEnabled()&&source.getBlockPos().distSqr(child.getBlockPos())<=(double)source.resonanceRange()*source.resonanceRange();
    }
    public static UUID root(EnergyBlockEntity node) {
        if(!(node.getLevel() instanceof ServerLevel level))return null;
        var seen=new HashSet<UUID>();
        while(node!=null&&node.kind()==EnergyContent.Kind.RESONATOR&&node.resonanceEnabled()&&!node.paused()&&seen.add(node.identity)) {
            if(node.resonanceParent==null)return node.identity;
            var parent=EnergyWorld.device(level,node.resonanceParent);
            if(parent==null||parent.kind()!=EnergyContent.Kind.RESONATOR||!isParent(parent,node)||!parent.sameOwner(node)||parent.channel!=node.channel||!inRange(parent,node))return null;
            node=parent;
        }
        return null;
    }
    /** Returns a translation suffix, or null when the proposed edge is valid. */
    public static String bind(EnergyBlockEntity source,EnergyBlockEntity child) {
        if(source==child)return "network_cycle";
        if(!source.sameOwner(child))return "denied";
        if(!source.resonanceEnabled())return "network_crystal";
        if(!inRange(source,child))return "out_of_range";
        if(child.kind().infinite()||child.kind()==EnergyContent.Kind.DISRUPTOR)return "network_target";
        if(!(source.getLevel() instanceof ServerLevel level))return "network_asleep";
        var seen=new HashSet<UUID>();var cursor=source;
        while(cursor!=null) {
            if(cursor==child||cursor.identity.equals(child.identity)||!seen.add(cursor.identity))return "network_cycle";
            if(cursor.resonanceParent==null)break;
            var parent=EnergyWorld.device(level,cursor.resonanceParent);
            if(parent==null||!isParent(parent,cursor))return "network_asleep";
            cursor=parent;
        }
        if(root(source)==null)return "network_asleep";
        child.resonanceParent=source.getBlockPos().immutable();child.resonanceParentIdentity=source.identity;
        child.channel=source.channel;child.changed();return null;
    }
    public static void detach(EnergyBlockEntity node) {node.resonanceParent=null;node.resonanceParentIdentity=null;node.changed();}
    public static boolean accepts(EnergyBlockEntity source,EnergyBlockEntity dest) {
        if(source.kind()==EnergyContent.Kind.RESONATOR)return isParent(source,dest);
        return dest.kind()!=EnergyContent.Kind.RESONATOR||dest.resonanceParent==null;
    }
    private ResonanceNetwork() {}
}

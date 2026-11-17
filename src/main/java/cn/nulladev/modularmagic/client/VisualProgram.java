package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.config.ResourceConfig;
import dev.xkmc.l2magic.content.engine.context.EngineContext;
import dev.xkmc.l2magic.content.engine.core.*;
import dev.xkmc.l2magic.content.engine.particle.ParticleInstance;
import net.minecraft.world.level.Level;
import java.lang.reflect.*;
import java.util.List;

/** Throttle particle creation before constructing/updating thousands of invisible excess particles. */
public final class VisualProgram {
    private static Level world;
    private static long tick;
    private static int used;
    public static ConfiguredEngine<?> limit(ConfiguredEngine<?> engine){return (ConfiguredEngine<?>)map(engine);}
    private static Object map(Object value){
        if(value instanceof ParticleInstance<?> particle)return new Particle(particle);
        if(value instanceof List<?> list)return list.stream().map(VisualProgram::map).toList();
        if(value==null||!value.getClass().isRecord()||!value.getClass().getPackageName().startsWith("dev.xkmc.l2magic"))return value;
        try{var fields=value.getClass().getRecordComponents();var args=new Object[fields.length];var types=new Class<?>[fields.length];
            for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();args[i]=map(fields[i].getAccessor().invoke(value));}
            return value.getClass().getDeclaredConstructor(types).newInstance(args);
        }catch(ReflectiveOperationException e){throw new IllegalArgumentException("Cannot prepare spell visuals",e);}
    }
    private static boolean allow(Level level){
        if(world!=level||tick!=level.getGameTime()){world=level;tick=level.getGameTime();used=0;}
        return used++<ResourceConfig.PARTICLES_PER_TICK.get();
    }
    private record Particle(ParticleInstance<?> delegate) implements ConfiguredEngine<Particle>{
        @Override @SuppressWarnings({"unchecked","rawtypes"}) public EngineType<Particle> type(){return (EngineType)delegate.type();}
        @Override public void execute(EngineContext ctx){if(allow(ctx.user().level()))delegate.execute(ctx);}
    }
    private VisualProgram(){}
}

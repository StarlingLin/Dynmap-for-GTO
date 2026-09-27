package org.dynmap.utils;
import java.util.Random;
import org.dynmap.modsupport.BlockSide;
import org.dynmap.modsupport.ModelBlockModel.SideRotation;
import org.dynmap.renderer.RenderPatchFactory.SideVisible;
public class UVBoundsTest {
    @org.junit.jupiter.api.Test public void independentCornerBoundsAndGtoFace() {
        Random r=new Random(27092026);
        int accept=0,reject=0;
        for(int k=0;k<20000;k++) {
            double[] o={r.nextDouble()*4-2,r.nextDouble()*4-2,r.nextDouble()*4-2};
            double[] u={r.nextDouble()*6-3,r.nextDouble()*6-3,r.nextDouble()*6-3};
            double[] v={r.nextDouble()*6-3,r.nextDouble()*6-3,r.nextDouble()*6-3};
            double u0=r.nextDouble(),u1=u0+r.nextDouble()*(1-u0);
            double v0=r.nextDouble(),v1=v0+r.nextDouble()*(1-v0);
            PatchDefinition p=new PatchDefinition();
            p.update(o[0],o[1],o[2],o[0]+u[0],o[1]+u[1],o[2]+u[2],o[0]+v[0],o[1]+v[1],o[2]+v[2],u0,u1,v0,v1,SideVisible.TOP,0,v0,v1,true);
            boolean want=true;
            for(double a:new double[]{u0,u1})for(double b:new double[]{v0,v1})for(int axis=0;axis<3;axis++){
                double x=o[axis]+a*u[axis]+b*v[axis];
                if(x < -1 || x > 2)want=false;
            }
            if(p.validate()!=want)throw new AssertionError("corner bounds mismatch at "+k);
            if(want)accept++;else reject++;
        }
        PatchDefinition p=new PatchDefinition();
        p.updateModelFace(new double[]{9,1,1},new double[]{11,15,15},BlockSide.EAST,new double[]{1,3,3,13},SideRotation.DEG0,true,0);
        if(!p.validate())throw new AssertionError("real GTO face rejected");
        p.updateModelFace(new double[]{0,0,0},new double[]{64,64,64},BlockSide.TOP,null,SideRotation.DEG0,true,0);
        if(p.validate())throw new AssertionError("out of range face accepted");
        System.out.printf("PASS: 20000 rectangular patches; accepted=%d rejected=%d; real-model and out-of-range checks passed%n",accept,reject);
    }
}

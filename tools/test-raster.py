import pathlib, subprocess, tempfile
source=pathlib.Path("app/src/main/java/com/iingstd/photoprint/Ipp.java").read_text()
methods=source[source.index(" static int rasterPixel("):source.index(" static void rasterPdf(")]
harness="""import java.io.*;import java.util.*;
class RasterFixture {
"""+methods+"""
static void check(int[] row,boolean gray)throws Exception{
 ByteArrayOutputStream b=new ByteArrayOutputStream();writeRasterRow(new DataOutputStream(b),row,gray);
 DataInputStream in=new DataInputStream(new ByteArrayInputStream(b.toByteArray()));
 if(in.readUnsignedByte()!=0)throw new AssertionError("row repeat");
 int x=0;
 while(x<row.length){
 int tag=in.readUnsignedByte();if(tag==128)throw new AssertionError("unexpected marker");
 int n=tag<128?tag+1:257-tag;
 if(x+n>row.length)throw new AssertionError("overflow");
 int pixel=0;
 for(int j=0;j<n;j++){
 if(j==0||tag>128){pixel=in.readUnsignedByte();if(!gray)pixel=(pixel<<16)|(in.readUnsignedByte()<<8)|in.readUnsignedByte();}
 int p=row[x++],expected=gray?((p>>>16&255)*299+(p>>>8&255)*587+(p&255)*114+500)/1000:p&0xffffff;
 if(pixel!=expected)throw new AssertionError("pixel mismatch");
 }}
 if(in.read()!=-1)throw new AssertionError("trailing data");
}
public static void main(String[] args)throws Exception{
 Random random=new Random(42);
 for(boolean gray:new boolean[]{false,true})for(int n:new int[]{1,2,127,128,129,255,256,257,2480}){
 int[] row=new int[n];check(row,gray);
 Arrays.fill(row,0xffffff);check(row,gray);
 for(int i=0;i<n;i++)row[i]=random.nextInt();check(row,gray);
 for(int i=0;i<n;i++)row[i]=(i/3)%2==0?0xff0000:0x00ff00;check(row,gray);
 }
 System.out.println("Raster row fixtures passed");
}
}
"""
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d)/"RasterFixture.java"
 p.write_text(harness)
 subprocess.run(["java",str(p)],check=True)

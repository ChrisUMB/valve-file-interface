import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector3f;

public class RGB565Test {

    public static void main(String[] args) {
        Vector3f red = new Vector3f(1f, 0f, 0f);
        Vector3f green = new Vector3f(0f, 1f, 0f);
        Vector3f blue = new Vector3f(0f, 0f, 1f);

        System.out.printf("%s%n", Integer.toString(Short.toUnsignedInt(ImageUtil.encodeRGB565(red)), 2));
        System.out.printf("%s%n", Integer.toString(Short.toUnsignedInt(ImageUtil.encodeRGB565(green)), 2));
        System.out.printf("%s%n", Integer.toString(Short.toUnsignedInt(ImageUtil.encodeRGB565(blue)), 2));
    }

}

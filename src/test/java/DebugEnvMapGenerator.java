import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class DebugEnvMapGenerator {

    public enum Face {
        FRONT(1, 0, 0, -90, true, false),
        BACK(-1, 0, 0, -90, false, true),
        LEFT(0, 1, 0, 0, false, true),
        RIGHT(0, -1, 0, 0, true, false),
        TOP(0, 0, 1, -90, true, false),
        BOTTOM(0, 0, -1, -90, true, false);

        private final int x, y, z;
        private final int rotation;
        private final boolean flipX, flipY;

        Face(int x, int y, int z, int rotation, boolean flipX, boolean flipY) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotation = rotation;
            this.flipX = flipX;
            this.flipY = flipY;
        }

        Vector3f vector() {
            return new Vector3f(x, y, z);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public int getRotation() {
            return rotation;
        }

        public boolean isFlipX() {
            return flipX;
        }

        public boolean isFlipY() {
            return flipY;
        }

        Face left() {
            return switch (this) {
                case FRONT, TOP, BOTTOM -> LEFT;
                case BACK -> RIGHT;
                case RIGHT -> FRONT;
                case LEFT -> BACK;
            };
        }

        Face right() {
            return switch (this) {
                case FRONT, TOP, BOTTOM -> RIGHT;
                case BACK -> LEFT;
                case RIGHT -> BACK;
                case LEFT -> FRONT;
            };
        }

        Face up() {
            return switch(this) {
                case FRONT, BACK, RIGHT, LEFT -> TOP;
                case TOP -> BACK;
                case BOTTOM -> FRONT;
            };
        }

        Face down() {
            return switch(this) {
                case FRONT, BACK, RIGHT, LEFT -> BOTTOM;
                case TOP -> FRONT;
                case BOTTOM -> BACK;
            };
        }

        Color toColor() {
            return new Color(getX() * 0.5f + 0.5f, getY() * 0.5f + 0.5f, getZ() * 0.5f + 0.5f);
        }
    }

    public static void main(String[] args) throws IOException {
        Face[] faces = Face.values();
        int size = 1024;
        for (int i = 0; i < faces.length; i++) {
            Face face = faces[i];
//            BufferedImage image = images[i];
//            BufferedImage original = ImageIO.read(new File("tests-in/bluepurple/" + face.name().toLowerCase(Locale.ROOT) + ".png"));
//            size = original.getWidth();
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

            BufferedImage original = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    float fx = (x / (float) size) * 2.0f - 1.0f;
                    float fy = (y / (float) size) * 2.0f - 1.0f;
                    Vector3f v1 = face.right().vector().mul(fx);
                    Vector3f v2 = face.down().vector().mul(fy);
                    Vector3f v = face.vector().add(v1).add(v2).normalize();
//                    Vector3f v = face.vector().normalize();
//                    Vector3f v = new Vector3f(fx, fy, 0).normalize();
                    original.setRGB(x, y, new Color(v.x * 0.5f + 0.5f, v.y * 0.5f + 0.5f, v.z * 0.5f + 0.5f).getRGB());
                }
            }

            Graphics2D graphics = image.createGraphics();
            AffineTransform transform = AffineTransform.getQuadrantRotateInstance(face.rotation / 90, size / 2.0, size / 2.0);
            if(face.flipX) {
                transform.scale(-1.0, 1.0);
                transform.translate(-size, 0.0);
            }
            if(face.flipY) {
                transform.scale(1.0, -1.0);
                transform.translate(0.0, -size);
            }
            graphics.setTransform(transform);
            int size8 = size / 16;
            graphics.drawImage(original, 0, 0, size, size, null);

            graphics.setBackground(face.toColor());
//            graphics.clearRect(0, 0, size, size);
            int indicatorSize = size8 * 1;
            int indicatorOffset = size8 * 1;
            int indicatorMin = indicatorOffset + indicatorSize;
            int indicatorMax = size - indicatorMin - indicatorSize;
            Face left = face.left();
            if (left != null) {
                graphics.setColor(left.toColor());
                graphics.fillRect(0, indicatorMin, indicatorSize, indicatorSize);
                graphics.fillRect(0, indicatorMax, indicatorSize, indicatorSize);
            }

            Face right = face.right();
            if (right != null) {
                graphics.setColor(right.toColor());
                graphics.fillRect(size - indicatorSize, indicatorMin, indicatorSize, indicatorSize);
                graphics.fillRect(size - indicatorSize, indicatorMax, indicatorSize, indicatorSize);
            }

            Face up = face.up();
            if(up != null) {
                graphics.setColor(up.toColor());
                graphics.fillRect(indicatorMin, 0, indicatorSize, indicatorSize);
                graphics.fillRect(indicatorMax, 0, indicatorSize, indicatorSize);
            }

            Face down = face.down();
            if(down != null) {
                graphics.setColor(down.toColor());
                graphics.fillRect(indicatorMin, size - indicatorSize, indicatorSize, indicatorSize);
                graphics.fillRect(indicatorMax, size - indicatorSize, indicatorSize, indicatorSize);
            }

            graphics.setColor(Color.WHITE);
            graphics.setFont(graphics.getFont().deriveFont((float) size8));
            Rectangle2D bounds = graphics.getFontMetrics().getStringBounds(face.name(), graphics);
            graphics.drawString(face.name(), (float) (size / 2.0f - bounds.getWidth() / 2.0f), (float) (size / 2.0f + bounds.getHeight() / 3.0f));
//            graphics.drawString(face.name(), size8, size / 2);

            graphics.dispose();
            ImageIO.write(image, "PNG", new File("tests-out/export/" + face.name().toLowerCase(Locale.ROOT) + ".png"));
        }
    }

}

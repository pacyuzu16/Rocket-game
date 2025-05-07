package game.obj;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

public class Bullet {

    private double x;
    private double y;
    private final Shape shape;
    private final Color color;
    private final float angle;
    private final double size;
    private float speed = 1f;

    public Bullet(double x, double y, float angle, double size, float speed, Color color) {
        x += Player.PLAYER_SIZE / 2 - (size / 2); // Center on player
        y += Player.PLAYER_SIZE / 2 - (size / 2);
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.size = size;
        this.speed = speed;
        this.color = color;
        // Define shape once, no resizing later
        this.shape = new RoundRectangle2D.Double(0, 0, size * 2, size / 2, size / 2, size / 2);
    }

    public void update() {
        x += Math.cos(Math.toRadians(angle)) * speed;
        y += Math.sin(Math.toRadians(angle)) * speed;
    }

    public boolean check(int width, int height) {
        return !(x <= -size || y < -size || x > width || y > height);
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(Math.toRadians(angle), size, size / 4); // Rotate around center
        g2.setColor(color);
        g2.fill(shape); // Shape remains constant
        g2.setTransform(oldTransform);
    }

    public Shape getShape() {
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.rotate(Math.toRadians(angle), size, size / 4);
        return new Area(at.createTransformedShape(shape));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getSize() {
        return size;
    }

    public double getCenterX() {
        return x + size;
    }

    public double getCenterY() {
        return y + size / 4;
    }
}
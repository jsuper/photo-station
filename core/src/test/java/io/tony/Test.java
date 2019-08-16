package io.tony;

public class Test {
  public static void main(String[] args) {
    byte b1 = 1;
    byte b2 = 0;
    byte b3 = 1;
    byte b4 = 0;

    int a = ((b1 & 0xFF) << 24) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 8) | ((b4 & 0xFF));

    System.out.println(Integer.toBinaryString((b1 & 0xff) << 24));
    System.out.println(Integer.toBinaryString((b2 & 0xff) << 16));
    System.out.println(Integer.toBinaryString((b3 & 0xff) << 8));
    System.out.println(((b3 & 0xff) << 8));
    System.out.println(Integer.toBinaryString(256));
    System.out.println(Integer.toBinaryString((b4 & 0xff)));

    System.out.println(0x7f);
    System.out.println(a);
  }
}

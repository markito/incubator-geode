package com.gemstone.gemfire.codeAnalysis.decode.cp;
import java.io.*;

public class CpFloat extends Cp {
    byte b1;
    byte b2;
    byte b3;
    byte b4;
    CpFloat( DataInputStream source ) throws IOException {
        b1 = (byte)source.readUnsignedByte();
        b2 = (byte)source.readUnsignedByte();
        b3 = (byte)source.readUnsignedByte();
        b4 = (byte)source.readUnsignedByte();
    }
}
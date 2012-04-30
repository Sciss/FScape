package de.sciss.fscape.op;

public class SlotAlreadyConnectedException
extends IllegalStateException
{
	public SlotAlreadyConnectedException() {}
	public SlotAlreadyConnectedException( String s ) { super( s );}
}

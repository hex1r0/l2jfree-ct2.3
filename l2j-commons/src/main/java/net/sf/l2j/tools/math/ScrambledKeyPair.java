/*
 * $HeadURL: $
 *
 * $Author: $
 * $Date: $
 * $Revision: $
 *
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.tools.math;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ScrambledKeyPair
{
    private final static Log _log = LogFactory.getLog(ScrambledKeyPair.class);
    
    private KeyPair pair;
    private byte[] scrambledModulus;
    
    public ScrambledKeyPair(KeyPair Pair)
    {
        this.pair = Pair;
        scrambledModulus = scrambleModulus( ((RSAPublicKey)this.pair.getPublic()).getModulus() );
    }
    
    private byte[] scrambleModulus(BigInteger modulus)
    {
        byte[] retScrambledModulus = modulus.toByteArray();

        if (retScrambledModulus.length == 0x81 && retScrambledModulus[0] == 0x00)
        {
            byte[] temp = new byte[0x80];
            System.arraycopy(retScrambledModulus, 1, temp, 0, 0x80);
            retScrambledModulus = temp;
        }
        // step 1 : 0x4d-0x50 <-> 0x00-0x04
        for (int i = 0; i < 4; i++)
        {
            byte temp = retScrambledModulus[0x00+i];
            retScrambledModulus[0x00+i] = retScrambledModulus[0x4d+i];
            retScrambledModulus[0x4d+i] = temp;
        }
        // step 2 : xor first 0x40 bytes with  last 0x40 bytes
        for (int i = 0; i < 0x40; i++)
        {
            retScrambledModulus[i] = (byte)(retScrambledModulus[i] ^ retScrambledModulus[0x40+i]);
        }
        // step 3 : xor bytes 0x0d-0x10 with bytes 0x34-0x38
        for (int i = 0; i < 4; i++)
        {
            retScrambledModulus[0x0d+i] = (byte)(retScrambledModulus[0x0d+i] ^ retScrambledModulus[0x34+i]);
        }
        // step 4 : xor last 0x40 bytes with  first 0x40 bytes
        for (int i=0;i<0x40;i++)
        {
            retScrambledModulus[0x40+i] = (byte)(retScrambledModulus[0x40+i] ^ retScrambledModulus[i]);
        }
        if ( _log.isDebugEnabled())_log.debug("Modulus was scrambled");
        
        return retScrambledModulus;
    }

    public KeyPair getPair()
    {
        return pair;
    }

    public void setPair(KeyPair pair)
    {
        this.pair = pair;
    }

    public byte[] getScrambledModulus()
    {
        return scrambledModulus;
    }

    public void setScrambledModulus(byte[] scrambledModulus)
    {
        this.scrambledModulus = scrambledModulus;
    }
}
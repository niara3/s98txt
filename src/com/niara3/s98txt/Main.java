package com.niara3.s98txt;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;

public class Main {

	private static final int NUM_DEVICE = 64;

	private static final int NUM_OPERATOR = 4;
	private static final int IDX_REG = 0;
	private static final int NUM_REG = 0xb3;
	private static final int IDX_FM_TONE = IDX_REG + NUM_REG;
	private static final int NUM_FM_TONE = 3;
	private static final int NUM_ALL_REG = IDX_FM_TONE + NUM_FM_TONE;
	private static final byte D_FF = (byte) 0xff;
	private static final byte D___ = (byte) 0xff;
	private static final byte[] regPowerOnReset_OPN = {
	//  +0   +1   +2   +3   +4   +5   +6   +7   +8   +9   +a   +b   +c   +d   +e   +f
		// REG ... レジスタの初期値（未調査なので値はテキトウ）
		D_FF,D_FF,D_FF,D_FF,D_FF,D_FF,0x00,                                             // +0 SSG IDX_REG_SSG_TONE_PERIOD hilo ch*3 
		                                   0x3f,                                        // +0 SSG IDX_REG_SSG_TONE_TOGGLE
		                                        D_FF,D_FF,D_FF,0x00,0x00,0x00,0x00,0x00,// +0 SSG IDX_REG_SSG_LEVEL ch*3
		D___,D___,D___,D___,D___,D___,D___,D___,D___,D___,D___,D___,D___,D___,D___,D___,// +1 (Reserve)
		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,                                        // +2 Timer
		                                        0x0f,0x00,0x00,0x00,0x00,0x00,0x00,0x00,// +2 IDX_REG_FM_SLOT_CH
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +3 FM IDX_REG_FM_DETUNE_MULTI ch*3 op*4
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +4 FM IDX_REG_FM_TOTAL_LEVEL ch*3 op*4
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +5 FM IDX_REG_FM_SCALE_ATTACK ch*3 op*4
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +6 FM IDX_REG_FM_DECAY ch*3 op*4
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +7 FM IDX_REG_FM_SUSTAIN_RATE ch*3 op*4
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +8 FM IDX_REG_FM_SUSTAIN_RELEASE ch*3 op*4
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +9 FM IDX_REG_FM_SSG_EG ch*3 op*4
		D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,                                        // +a FM IDX_REG_FM_BLOCK_F_NUM ch*3 hilo
		                                        D_FF,D_FF,D_FF,D___,D_FF,D_FF,D_FF,D___,// +a FM IDX_REG_FM_3CH_BLOCK_F_NUM op*3(ch3-op4,2,3) hilo
		D_FF,D_FF,D_FF,                                                                 // +b FM IDX_REG_FM_FEEDBACK_ALGO ch*3
		// FM_NOTE ... チャンネルごとの0x28レジスタ値
		0x00,0x00,0x00,                                                                 // IDX_REG_FM_SLOT_CH's ch*3
	};
	private static byte[][] regPrvs;
	private static byte[][] regNows;
	private static LinkedList<ByteBuffer> toneColorList = new LinkedList<ByteBuffer>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (0 >= args.length)
		{
			System.out.println("Usage: dmscript.exe s98.dms <s98fie>");
			return;
		}
		FileInputStream in = null;
		FileWriter out = null;
		BufferedWriter bw = null;
		try {
			{
				String inFileName = args[0];
				String outFileName = null;
				{
					int index = inFileName.lastIndexOf(".");
					if (0 < index)
					{
						outFileName = inFileName.substring(0, index) + ".txt";
					}
					else
					{
						outFileName = inFileName + ".txt";
					}
				}
				in = new FileInputStream(inFileName);
				System.out.println("inFile: " + inFileName);
				out = new FileWriter(outFileName);
				bw = new BufferedWriter(out);
				System.out.println("outFile: " + outFileName);
			}
			exe(in, bw);
		} catch (Exception e) {	// 雑に拾いすぎか？
			e.printStackTrace();
		} finally {
			if (null != in)
			{
				try {
					in.close();
					System.out.println("inFile: close");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != bw)
			{
				try {
					bw.close();
					System.out.println("inFile: close bw");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != out)
			{
				try {
					out.close();
					System.out.println("inFile: close out");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != regNows)
			{
				for (int i=0; i < NUM_DEVICE; i ++)
				{
					regNows[i] = null;
				}
			}
			if (null != regPrvs)
			{
				for (int i=0; i < NUM_DEVICE; i ++)
				{
					regPrvs[i] = null;
				}
			}
		}
	}

	private static void exe(FileInputStream in, BufferedWriter bw) {
		String errorStr = null;
		byte[] buf = new byte[1024];
		try {
			errorStr = "inFile: read HEADER error";
			in.read(buf, 0, 0x20);
			System.out.println("inFile: read HEADER");

			errorStr = "outFile: write HEADER dump error";
			bw.write("// outFile:" + String.format("%02x", buf[0]));
			for (int i=1; i < 0x20; i++)
			{
				bw.write(String.format(",%02x", buf[i]));
			}
			bw.write("\n");
			System.out.println("outFile: HEADER dump write");

			errorStr = "outFile: write HEADER error";
			int[] intbuf = new int[7];
			cnvLEs(buf, 4, intbuf, 7);

			bw.write("[HEADER FORMAT]\n");
			bw.write("MAGIC: 'S98'\nFORMAT VERSION '3'\n");
			bw.write(String.format("TIMER INFO: %08x\n", intbuf[0]));
			bw.write(String.format("TIMER INFO2: %08x\n", intbuf[1]));
			bw.write(String.format("COMPRESSING: %08x\n", intbuf[2]));
			bw.write(String.format("FILE OFFSET TO TAG: %08x\n", intbuf[3]));
			bw.write(String.format("FILE OFFSET TO DUMP DATA: %08x\n", intbuf[4]));
			bw.write(String.format("FILE OFFSET TO LOOP POINT DUMP DATA: %08x\n", intbuf[5]));
			bw.write(String.format("DEVICE COUNT: %08x\n", intbuf[6]));

			int adr = intbuf[4];
			int sizeTag = adr - 0x30;
			int numDevice = intbuf[6];

			if (0x53 != buf[0] || 0x39 != buf[1] || 0x38 != buf[2])
			{
				System.out.println("inFile: MAGIC error");
				return;
			}
			if (0x33 != buf[3])
			{
				System.out.println("inFile: FORMAT VERSION error");
				return;
			}
			if (0x0 >= numDevice || NUM_DEVICE < numDevice)
			{
				System.out.println("inFile: DEVICE COUNT error (1-64)");
				return;
			}

			int num = 0x10 * numDevice;

			errorStr = "inFile: read HEADER(device) error";
			in.read(buf, 0, num);
			System.out.println(String.format("inFile: read HEADER(device) %d", num));

			errorStr = "outFile: write HEADER(device) error";
			bw.write("// outFile:" + String.format("%02x", buf[0]));
			for (int i=1; i < num; i++)
			{
				bw.write(String.format(",%02x", buf[i]));
			}
			bw.write("\n");

			intbuf = new int[4];
			for (int cntDevice=0; cntDevice < numDevice; cntDevice++)
			{
				cnvLEs(buf, 4*4 * cntDevice, intbuf, 4);
				bw.write(String.format("[DEVICE INFO #%d]\n", cntDevice+1));
				bw.write(String.format("DEVICE TYPE: %08x\n", intbuf[0]));
				bw.write(String.format("CLOCK(Hz): %08x\n", intbuf[1]));
				bw.write(String.format("PAN: %08x\n", intbuf[2]));
				bw.write(String.format("RESERVE: %08x\n", intbuf[3]));
				if (2 != intbuf[1])	// OPNでない
				{
					System.out.println("inFile: DEVICE TYPE error (OPN only)");
					return;	// TODO: OPNA(SB2)やOPM(X68k)にも対応したいところ
				}
			}
			System.out.println("outFile: write HEADER(device)");

			if (0 > sizeTag)
			{
				System.out.println("inFile: FILE OFFSET TO DUMP DATA error (> 0x30)");
				return;
			}
			if (0 < sizeTag)	// TODO: 動作確認
			{
				errorStr = "inFile: read TAG error";
				in.read(buf, 0, sizeTag);
				System.out.println("inFile: read TAG");

				Charset charset = StandardCharsets.UTF_8;
				if (0xef != buf[0])
				{
					charset = Charset.forName("MS932");
				}
				bw.write("[TAG]\n");
				bw.write(new String(buf, 0, sizeTag, charset));
				System.out.println("outFile: write TAG");
			}
			bw.write("[DUMP DATA FORMAT]\n");

			regPrvs = new byte[NUM_DEVICE][];
			regNows = new byte[NUM_DEVICE][];

			int offset = 0;
			do
			{
				// s98読み込み
				errorStr = "inFile: read DUMP DATA FORMAT error";
				int size = in.read(buf, offset, buf.length - offset);
				if (0 >= size)
				{
					break;
				}
				size += offset;
				System.out.println(String.format("inFile: read DUMP DATA FORMAT size=%d", size));

				// txt書き出し　戻り値：負=エラー、0=成功（余りデータなし）、正=成功（余りデータ数）
				errorStr = "outFile: write DUMP DATA FORMAT error";
				offset = exeDumpData(buf, size, adr, bw);
				if (0 > offset)
				{
					break;
				}
				System.out.println(String.format("outFile: write DUMP DATA FORMAT left=%d", offset));
				adr += size - offset;
				if (0 < offset)
				{
					System.arraycopy(
							buf, size - offset,	// src
							buf, 0,	// dst
							offset);
				}
			} while (0 <= offset);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(errorStr);
		}
	}

	private static final int IDX_REG_FM_SLOT_CH = 0x28;

	private static int exeDumpData(byte[] buf, int size, int adr, BufferedWriter bw) throws IOException {
		int index = 0;
		int indexTop;
		while (index < size)
		{
			indexTop = index;
			
			int opc = 0xff & buf[index];
			index ++;
			int sync = 0;	// TODO: 1あたりの実時間算出==>Timerレジスタ値の解析？

			if (0xff == opc)
			{
				System.out.println(String.format("%06x: FF", adr+indexTop));
				sync = 1;
			}
			else if (0xfe == opc)
			{
				System.out.print(String.format("%06x: FE", adr+indexTop));
				int shift = 0;
				byte value;
				do
				{
					if(index >= size)
					{
						return size - indexTop;
					}
					value = buf[index];
					index ++;

					System.out.print(String.format(" %02X", value));
					sync |= ((int)(value & 0x7f)) << shift;
					shift += 7;
				}
				while (0 != (value & 0x80));
				System.out.println("");

				if (0 >= sync)
				{
					System.out.println(String.format("%06x: FD", adr+indexTop));
					return -1;
				}
			}
			else if (0xfd == opc)
			{
				bw.write(String.format("%06x: END/LOOP\n", adr+indexTop));
				return size - index;
			}
			else if (NUM_DEVICE <= opc)
			{
				System.out.println(String.format("inFile: DEVICE number error %d", opc));
				return -1;
			}

			if (0 < sync)
			{
				for (int dev=0; dev < NUM_DEVICE; dev ++)
				{
					byte[] regNow = regNows[dev];
					if (null == regNow)
					{
						continue;
					}

					String strAdr = String.format("%06x: ", adr+indexTop);
					FM_Tone(bw, strAdr, dev, 1);
					FM_Tone(bw, strAdr, dev, 2);
					FM_Tone(bw, strAdr, dev, 3);
					SSG_Tone(bw, strAdr, dev, 4);
					SSG_Tone(bw, strAdr, dev, 5);
					SSG_Tone(bw, strAdr, dev, 6);
	
					byte[] regPrv = regPrvs[dev];
					if (null == regPrv)
					{
						regPrv = new byte[NUM_ALL_REG];
						regPrvs[dev] = regPrv;
					}
					System.arraycopy(
							regNow, 0,	// src
							regPrv, 0,	// dst
							NUM_ALL_REG);
				}
				bw.write(String.format("%06x: %d SYNC\n", adr+indexTop, sync));
			}
			else
			{
				int left = size - indexTop;
				if (3 > left)
				{
					return left;
				}
				byte[] regNow = regNows[opc];
				if (null == regNow)
				{
					regNow = new byte[NUM_ALL_REG];
					regNows[opc] = regNow;
					System.arraycopy(
							regPowerOnReset_OPN, 0,	// src
							regNow, 0,	// dst
							NUM_ALL_REG);
				}

				int aa = 0xff & (int)buf[index];
				index ++;
				byte dd = buf[index];
				index ++;

				if (NUM_REG <= aa)
				{
					System.out.println(String.format("inFile: REG number error 0x%02x", aa));
					return -1;
				}

				System.out.println(String.format("%06x: %02X %02X %02X", adr+indexTop, opc, aa, dd));
				regNow[aa] = dd;

				if (IDX_REG_FM_SLOT_CH == aa)
				{
					System.out.println(String.format("%06x: FM_TONE[%d] %02X", adr+indexTop, (0x03 & dd), dd));
					regNow[IDX_FM_TONE + (0x03 & dd)] = dd;
				}
			}
		}

		return 0;
	}

	private static final int IDX_REG_FM_DETUNE_MULTI = 0x30;
	private static final int IDX_REG_FM_TOTAL_LEVEL = 0x40;
	private static final int IDX_REG_FM_SCALE_ATTACK = 0x50;
	//private static final int IDX_REG_FM_DECAY = 0x60;				// TODO: ToneColor_FMへ
	//private static final int IDX_REG_FM_SUSTAIN_RATE = 0x70;		// TODO: ToneColor_FMへ
	//private static final int IDX_REG_FM_SUSTAIN_RELEASE = 0x80;	// TODO: ToneColor_FMへ
	//private static final int IDX_REG_FM_SSG_EG = 0x90;			// TODO: ToneColor_FMへ
	private static final int IDX_REG_FM_BLOCK_F_NUM = 0xA0;
	private static final int IDX_REG_FM_FEEDBACK_ALGO = 0xB0;
	private static enum ToneColor_FM {	// 音色情報のインデックス
		IDX_OP1_DETUNE,			IDX_OP2_DETUNE,			IDX_OP3_DETUNE,			IDX_OP4_DETUNE,
		IDX_OP1_MULTIPLE,		IDX_OP2_MULTIPLE,		IDX_OP3_MULTIPLE,		IDX_OP4_MULTIPLE,
		IDX_OP1_TOTAL_LEVEL,	IDX_OP2_TOTAL_LEVEL,	IDX_OP3_TOTAL_LEVEL,	IDX_OP4_TOTAL_LEVEL,
		IDX_OP1_KEY_SCALE,		IDX_OP2_KEY_SCALE,		IDX_OP3_KEY_SCALE,		IDX_OP4_KEY_SCALE,
		IDX_OP1_ATTACK_RATE,	IDX_OP2_ATTACK_RATE,	IDX_OP3_ATTACK_RATE,	IDX_OP4_ATTACK_RATE,
		IDX_FEEDBACK,
		IDX_ALGORITHM,
		NUM,
	};
	private static enum RegCh_FM {	// FMチャンネル情報のインデックス
		IDX_TONE_COLOR,			// 動的に採番した音色番号
		IDX_LAST_TOTAL_LEVEL,	// ONな最大OPのボリューム　TODO: これでいいのか？
		IDX_BLOCK_F_NUM,		// 音程
		NUM
	};

	private static void FM_Tone(BufferedWriter bw, String strAdr, int dev, int ch) throws IOException {
		int cho = ch - 1;
		byte[] regNow = regNows[dev];
		byte[] regPrv = regPrvs[dev];
		short[] regChNow = null;
		short[] regChPrv = null;
		if (null == regPrv)
		{
			regPrv = regPowerOnReset_OPN;
		}
		int valRegNow = 0xf0 & regNow[IDX_FM_TONE + cho];
		int valRegPrv = 0xf0 & regPrv[IDX_FM_TONE + cho];
		if (0 == valRegNow)
		{	// 今OFF
			if (0 == valRegPrv)
			{	// 前OFF
				return;
			}
		}
		else
		{	// 今ON
			regChNow = getFMCh(regNow, cho, 0x0f & (valRegNow >> 4));
			regChPrv = getFMCh(regPrv, cho, 0x0f & (valRegPrv >> 4));
			if (0 != valRegPrv)
			{	// 前ON
				if (Arrays.equals(regChPrv, regChNow))	// 一致
				{
					return;
				}
			}
		}
		bw.write(strAdr);
		bw.write(String.format("#%02d FM  ch.%d ", dev+1, ch));
		if (0 == valRegNow)
		{
			bw.write("OFF\n");
		}
		else
		{
			//if (valRegPrv != valRegNow)
			//{
				bw.write("ON");
			//}
			//else
			//{
			//	bw.write("--");
			//}
			valRegPrv = regChPrv[RegCh_FM.IDX_TONE_COLOR.ordinal()];
			valRegNow = regChNow[RegCh_FM.IDX_TONE_COLOR.ordinal()];
			if (valRegPrv != valRegNow)
			{
				bw.write(String.format(" @%03d", valRegNow));
			}
			else
			{
				bw.write(" ----");
			}
			valRegPrv = regChPrv[RegCh_FM.IDX_LAST_TOTAL_LEVEL.ordinal()];
			valRegNow = regChNow[RegCh_FM.IDX_LAST_TOTAL_LEVEL.ordinal()];
			if (valRegPrv != valRegNow)
			{
				bw.write(String.format(" Lv%03d", valRegNow));
			}
			else
			{
				bw.write(" -----");
			}
			valRegPrv = regChPrv[RegCh_FM.IDX_BLOCK_F_NUM.ordinal()];
			valRegNow = regChNow[RegCh_FM.IDX_BLOCK_F_NUM.ordinal()];
			if (valRegPrv != valRegNow)
			{
				bw.write(String.format(" %04x %s\n", 0x3fff & valRegNow, getFMTone(valRegNow)));
			}
			else
			{
				bw.write(" -----\n");
			}
		}
	}

	private static final String[] Tones = {
		"c",
		"c+",
		"d",
		"d+",
		"e",
		"f",
		"f+",
		"g",
		"g+",
		"a",
		"a+",
		"b",
	};

	private static final int[] FM_F_Numbers = {
		0x26a,	// c
		0x28f,	// c+
		0x2b6,	// d
		0x2df,	// d+
		0x30b,	// e
		0x339,	// f
		0x36a,	// f+
		0x39e,	// g
		0x3d5,	// g+
		0x410,	// a
		0x44e,	// a+
		0x48f,	// b
	};

	private static String getFMTone(final int toneFM) {	//変
		int hi, lo;
		for (int t=FM_F_Numbers.length-1; t>0; t --)
		{
			hi = 0x7ff & FM_F_Numbers[t];
			lo = 0x7ff & FM_F_Numbers[t-1];
			if (toneFM > hi - (hi - lo) / 2)
			{
				return String.format("o%d%s", (toneFM >> 11)+1, Tones[Tones.length-1-t]);
			}
		}
		return String.format("o%d", (toneFM >> 11)+1);
	}

	private static short[] getFMCh(byte[] reg, int cho, int slot) {
		short[] regCh = new short[RegCh_FM.NUM.ordinal()];
		ByteBuffer toneColor = ByteBuffer.allocate(ToneColor_FM.NUM.ordinal());
		int value;

		value = reg[IDX_REG_FM_FEEDBACK_ALGO+cho];
		toneColor.put(ToneColor_FM.IDX_FEEDBACK.ordinal(), (byte)(0x07 & (value >> 3)));
		toneColor.put(ToneColor_FM.IDX_ALGORITHM.ordinal(), (byte)(0x07 & value));

		for (int op=0; op < NUM_OPERATOR; op ++)
		{
			if (0 == ((1 << op) & slot))
			{
				continue;	// 0 keep
			}

			value = 0x3f & (reg[IDX_REG_FM_DETUNE_MULTI+cho]);
			toneColor.put(ToneColor_FM.IDX_OP1_DETUNE.ordinal()+op, (byte)(0x0f & (value >> 4)));
			toneColor.put(ToneColor_FM.IDX_OP1_MULTIPLE.ordinal()+op, (byte)(0x0f & value));

			value = 127 - reg[IDX_REG_FM_TOTAL_LEVEL+cho+4*op];
			toneColor.put(ToneColor_FM.IDX_OP1_TOTAL_LEVEL.ordinal()+op, (byte)value);
			regCh[RegCh_FM.IDX_LAST_TOTAL_LEVEL.ordinal()] = (short)value;

			value = 0x3f & (reg[IDX_REG_FM_SCALE_ATTACK+cho]);
			toneColor.put(ToneColor_FM.IDX_OP1_KEY_SCALE.ordinal(), (byte)(0x0f & (value >> 4)));
			toneColor.put(ToneColor_FM.IDX_OP1_ATTACK_RATE.ordinal(), (byte)(0x0f & value));
		}

		int toneColorNumber = toneColorList.indexOf(toneColor);
		if (0 > toneColorNumber)
		{
			toneColorList.add(toneColor);
			toneColorNumber = toneColorList.size() - 1;
		}
		regCh[RegCh_FM.IDX_TONE_COLOR.ordinal()] = (short)toneColorNumber;

		value =
				(0x3f & reg[IDX_REG_FM_BLOCK_F_NUM+4+cho]) *256 +
				(0xff & reg[IDX_REG_FM_BLOCK_F_NUM+0+cho]);
		regCh[RegCh_FM.IDX_BLOCK_F_NUM.ordinal()] = (short)value;

		return regCh;
	}

	private static final int IDX_REG_SSG_TONE_PERIOD = 0x00;
	private static final int IDX_REG_SSG_NOISE_TONE = 0x07;
	private static final int IDX_REG_SSG_M_LEVEL = 0x08;
	private static enum RegCh_SSG {
		IDX_TONE_PERIOD,
		IDX_LEVEL,
		NUM
	};

	private static void SSG_Tone(BufferedWriter bw, String strAdr, int dev, int ch) throws IOException {
		int cho = ch - 4;
		byte[] regNow = regNows[dev];
		byte[] regPrv = regPrvs[dev];
		short[] regChNow = null;
		short[] regChPrv = null;
		if (null == regPrv)
		{
			regPrv = regPowerOnReset_OPN;
		}
		int mask = 1 << cho;
		int valRegNow = mask & regNow[IDX_REG_SSG_NOISE_TONE];
		int valRegPrv = mask & regPrv[IDX_REG_SSG_NOISE_TONE];
		if (0 != valRegNow)
		{	// 今OFF
			if (0 != valRegPrv)
			{	// 前OFF
				return;
			}
		}
		else
		{	// 今ON
			regChNow = getSSGCh(regNow, cho);
			regChPrv = getSSGCh(regPrv, cho);
			if (0 == valRegPrv)
			{	// 前ON
				if (Arrays.equals(regChPrv, regChNow))	// 一致
				{
					return;
				}
			}
		}
		bw.write(strAdr);
		bw.write(String.format("#%02d SSG ch.%d ", dev+1, ch));
		if (0 != valRegNow)
		{
			bw.write("OFF\n");
		}
		else
		{
			//if (valRegPrv != valRegNow)
			//{
				bw.write("ON");
			//}
			//else
			//{
			//	bw.write("--");
			//}
			valRegPrv = regChPrv[RegCh_SSG.IDX_LEVEL.ordinal()];
			valRegNow = regChNow[RegCh_SSG.IDX_LEVEL.ordinal()];
			if (valRegPrv != valRegNow)
			{
				bw.write(String.format(" Lv%02d", valRegNow));
			}
			else
			{
				bw.write(" ----");
			}
			valRegPrv = regChPrv[RegCh_SSG.IDX_TONE_PERIOD.ordinal()];
			valRegNow = regChNow[RegCh_SSG.IDX_TONE_PERIOD.ordinal()];
			if (valRegPrv != valRegNow)
			{
				bw.write(String.format(" %04x %s\n", valRegNow, getSSGTone(valRegNow)));
			}
			else
			{
				bw.write(" ----\n");
			}
		}
	}

	//private static int getReg1(byte[] regPrv, byte opc, int indexReg, int highBit, int lowBit) {
	//	return 0xff & ((regPrv[indexReg] & ~(-1 << (highBit+1)) & ~(-1 << lowBit)) >> lowBit);
	//}

	private static final short[] SSG_o1_Tones = {
		0xee8,	// o1 c
		0xe12,	// o1 c+
		0xd48,	// o1 d
		0xc89,	// o1 d+
		0xbd5,	// o1 e
		0xb2b,	// o1 f
		0xa8a,	// o1 f+
		0x9f3,	// o1 g
		0x964,	// o1 g+
		0x8dd,	// o1 a
		0x85e,	// o1 a+
		0x7e6,	// o1 b
	};

	private static String getSSGTone(final int toneSSG) {
		for (int o=0; o<8; o ++)
		{
			int hi, lo;
			for (int t=1; t<SSG_o1_Tones.length; t ++)
			{
				hi = SSG_o1_Tones[t-1] >> o;
				lo = SSG_o1_Tones[t] >> o;
				if (toneSSG > hi - (hi - lo) / 2)
				{
					return String.format("o%d%s", o+1, Tones[t-1]);
				}
			}
			if (7<=o)
			{
				break;
			}
			hi = SSG_o1_Tones[SSG_o1_Tones.length-1] >> o;
			lo = SSG_o1_Tones[0] >> (o+1);
			if (toneSSG > hi - (hi - lo) / 2)
			{
				return String.format("o%d%s", o+1, Tones[SSG_o1_Tones.length-1]);
			}
		}
		return "o8b";
	}

	private static short[] getSSGCh(byte[] reg, int cho) {
		short[] regCh = new short[RegCh_SSG.NUM.ordinal()];
		int value;

		value =
				(0x3f & reg[IDX_REG_SSG_TONE_PERIOD+1+cho*2]) * 256 +
				(0xff & reg[IDX_REG_SSG_TONE_PERIOD+0+cho*2]);
		regCh[RegCh_SSG.IDX_TONE_PERIOD.ordinal()] = (short)value;

		value = 0x1f & reg[IDX_REG_SSG_M_LEVEL+cho];
		regCh[RegCh_SSG.IDX_LEVEL.ordinal()] = (short)(0x0f & value);
		return regCh;
	}

	private static void cnvLEs(final byte[] buf, int offset, int[] intbuf, int num) {
		for (int i=0; i<num; i++)
		{
			intbuf[i] = cnvLE(buf, offset + i * 4);
		}
	}

	private static int cnvLE(final byte[] buf, int offset) {
		long value = ((long)buf[offset+3]) << 12;
		value += ((long)buf[offset+2]) << 8;
		value += ((long)buf[offset+1]) << 4;
		value += (long)buf[offset+0];
		return (int)value;
	}

}

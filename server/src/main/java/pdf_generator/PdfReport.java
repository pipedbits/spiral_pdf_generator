package pdf_generator;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.io.*;
import java.math.BigDecimal;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.AcroFields.FieldPosition;
import com.itextpdf.text.pdf.qrcode.*;
import com.itextpdf.text.html.*;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

/**
 * PDFĢɼ�ѤΥ��饹
 */
public class PdfReport
{
	// �ǡ������Ϸ���
	public static final int OUTPUT_VALUE	= 1;	// ��
	public static final int OUTPUT_IMAGE	= 2;	// ����
	public static final int OUTPUT_QR		= 3;	// QR������

	// �������Ϸ���
	public static final int KEEP_WH_RATIO	= 1;	// �Ĳ����ݻ���������˼����
	public static final int FIT_FRAME		= 2;	// �Ȥ˥ԥå�������
	public static final int FIT_HEIGHT		= 3;	// �Ȥι⤵�˹�碌��
	public static final int FIT_WIDTH		= 4;	// �Ȥ����˹�碌��
	public static final int ORIGINAL		= 5;	// ���Τޤ�

	// QR�����ɸ��������٥�
	public static final int LV_L =	1;	//  7%
	public static final int LV_M =	2;	// 15%
	public static final int LV_Q =	3;	// 25%
	public static final int LV_H =	4;	// 30%

	// �Ź沽����
	public static final int ENC_RC4_40	= 1;	// 40-bit RC4
	public static final int ENC_RC4_128	= 2;	// 128-bit RC4
	public static final int ENC_AES_128	= 3;	// 128-bit AES
	public static final int ENC_AES_256	= 4;	// 256-bit AES

	// ����
	public static final int ALLOW_SCREENREADERS			= 0x00000001;
	public static final int ALLOW_COPY					= 0x00000010;
	public static final int ALLOW_PRINTING				= 0x00000100;
	public static final int ALLOW_ASSEMBLY				= 0x00001000;
	public static final int ALLOW_DEGRADED_PRINTING		= 0x00010000;
	public static final int ALLOW_FILL_IN				= 0x00100000;
	public static final int ALLOW_MODIFY_CONTENTS		= 0x01000000;
	public static final int ALLOW_MODIFY_ANNOTATIONS	= 0x10000000;

	private String m_fileName = null;
	private String m_ownPass = null;
	private String m_usrPass = null;
	private int m_permission = 0;
	private int m_encryption = 0;
	private JSONArray m_dataList = null;
	private PdfReader m_reader = null;

	/**
	 * ���󥹥ȥ饯��
	 *
	 * @param _template PDF�ƥ�ץ졼��
	 * @param _settings PDF������
	 */
	public PdfReport(byte[] _template,
					 JSONObject _settings)
		throws Exception
	{
		// ���������å�
		if (_template == null) {
			throw new Exception("_template is null.");
		}
		if (_settings == null) {
			throw new Exception("_settings is null.");
		}

		// �ƥ�ץ졼��
		m_reader = new PdfReader(_template);

		m_fileName = _settings.getString("fileName");
		m_ownPass = _settings.getString("ownPass");
		m_usrPass = _settings.getString("usrPass");
		String permission = _settings.getString("permission");
		if (permission != null) {
			int perm = Integer.parseInt(permission);
			if ((perm & ALLOW_SCREENREADERS) != 0) {
				m_permission |= PdfWriter.ALLOW_SCREENREADERS;
			}
			if ((perm & ALLOW_COPY) != 0) {
				m_permission |= PdfWriter.ALLOW_COPY;
			}
			if ((perm & ALLOW_PRINTING) != 0) {
				m_permission |= PdfWriter.ALLOW_PRINTING;
			}
			if ((perm & ALLOW_ASSEMBLY) != 0) {
				m_permission |= PdfWriter.ALLOW_ASSEMBLY;
			}
			if ((perm & ALLOW_DEGRADED_PRINTING) != 0) {
				m_permission |= PdfWriter.ALLOW_DEGRADED_PRINTING;
			}
			if ((perm & ALLOW_FILL_IN) != 0) {
				m_permission |= PdfWriter.ALLOW_FILL_IN;
			}
			if ((perm & ALLOW_MODIFY_CONTENTS) != 0) {
				m_permission |= PdfWriter.ALLOW_MODIFY_CONTENTS;
			}
			if ((perm & ALLOW_MODIFY_ANNOTATIONS) != 0) {
				m_permission |= PdfWriter.ALLOW_MODIFY_ANNOTATIONS;
			}
		}
		String encryption = _settings.getString("encryption");
		if (encryption != null) {
			int enc = Integer.parseInt(encryption);
			switch (enc) {
				case ENC_AES_256:
					m_encryption = PdfWriter.ENCRYPTION_AES_256;
					break;
				case ENC_AES_128:
					m_encryption = PdfWriter.ENCRYPTION_AES_128;
					break;
				case ENC_RC4_128:
					m_encryption = PdfWriter.STANDARD_ENCRYPTION_128;
					break;
				default:
				case ENC_RC4_40:
					m_encryption = PdfWriter.STANDARD_ENCRYPTION_40;
					break;
			}
		}

		String dataList = _settings.getString("dataList");
		if (dataList != null) {
			m_dataList = JSONArray.fromObject(dataList);
		}
	}

	/**
	 * PDFĢɼ����
	 *
	 * @param _res �����֥�åȥ쥹�ݥ�
	 */
	public void output(HttpServletResponse _res)
		throws IOException, DocumentException
	{
		ServletOutputStream objOut = null;

		try {
			// �����Ѥ�Stream�򥤥󥹥��󥹲�
			ByteArrayOutputStream byteout = new ByteArrayOutputStream();

			// �ƥ�ץ졼���Խ���
			PdfStamper stamper = new PdfStamper(m_reader, byteout);

			// �ѥ�������꤬������
			if ((m_usrPass != null && m_usrPass.length() > 0) || (m_ownPass != null && m_ownPass.length() > 0)) {
				// �桼�����ѥ��Ϥ��äƥ����ʡ��ѥ���̵�����ϥ����ʡ��ѥ���桼�����ѥ���Ʊ�첽
				if (m_usrPass != null && m_usrPass.length() > 0 && (m_ownPass == null || m_ownPass.length() < 1)) {
					m_ownPass = m_usrPass;
				}

				// PDF��Ź沽
				stamper.setEncryption(m_encryption, m_usrPass, m_ownPass, m_permission);
			}

			// �ƥ�ץ졼����Υե�����ɷ������
			AcroFields fields = stamper.getAcroFields();

			// �ե�����ɤ˥ǡ����򥻥å�
			setData(stamper, fields);

			// �ե������ʿ�̲����Խ��ԲĲ���
			stamper.setFormFlattening(true);

			// �Խ���λ
			stamper.close();

			// PDF��Х��ʥ�ǡ����Ȥ��ƥ��饤����Ȥ˽���
			byte[] buf = byteout.toByteArray();
			_res.setContentType("application/pdf");
			_res.setHeader("Content-disposition","attachment; filename=\"" + m_fileName + "\"");
			_res.setContentLength(buf.length);

			objOut = _res.getOutputStream();
			objOut.write(buf);
			objOut.flush();
			objOut.close();
		} finally {
			if (objOut != null) {
				try {
					objOut.close();
				} catch (IOException e) {
					/* ignore */
				}
				objOut = null;
			}
		}
	}

	/**
	 * �ƥ�ץ졼����إǡ������å�
	 */
	private void setData(PdfStamper _stamper,
						 AcroFields _fields)
		throws IOException, DocumentException
	{
		int listSize = m_dataList.size();
		int oType = -1;
		int imgStyle = -1;
		int corrLv = -1;
		int fieldType = -1;
		String oTypeStr = null;
		String tgtName = null;
		String value = null;
		JSONObject jsonData = null;
		Image img = null;
		List<AcroFields.FieldPosition> posList = null;

		// �ǡ����ꥹ�ȤΥ�����ʬ�����롼��
		for (int i = 0; i < listSize; i++) {
			// �ǡ�����JSON�����Ǽ���
			jsonData = JSONObject.fromObject(m_dataList.get(i));

			// �ǡ������Ϸ��������
			oTypeStr = jsonData.getString("output_type");
			if (oTypeStr != null) {
				oType = Integer.parseInt(oTypeStr);
			}

			// �������å�̾�ʥƥ�ץ�������֤��줿�ե�����̾�˼���
			tgtName = jsonData.getString("target_name");

			// �������åȤ����ƹ�����
			value = jsonData.getString("value");

			// �������åȤ��ƥ����ȥե�����ɤǤʤ���в��⤷�ʤ�
			fieldType = _fields.getFieldType(tgtName);
			if (fieldType != AcroFields.FIELD_TYPE_TEXT) {
				continue;
			}

			// �ǡ������Ϸ����ˤ��ʬ��
			switch (oType) {

				// �ͤ򤽤Τޤ�ɽ��
				case OUTPUT_VALUE:
					_fields.setField(tgtName, value);
					break;

				// �����Ȥ���ɽ��
				case OUTPUT_IMAGE:
					imgStyle = Integer.parseInt(jsonData.getString("image_style"));
					img = Image.getInstance(value.getBytes());
					posList = _fields.getFieldPositions(tgtName);
					setImage(_stamper, posList, img, imgStyle);
					break;

				// QR�����ɤȤ���ɽ��
				case OUTPUT_QR:
					corrLv = Integer.parseInt(jsonData.getString("correction_lv"));
					posList = _fields.getFieldPositions(tgtName);
					setQR(_stamper, posList, value, corrLv);
					break;

				// �ǥե����
				default:
					break;
			}
		}
	}

	/**
	 * �������å�
	 *
	 * @param _stamper �ƥ�ץ졼���Խ��ѥ��饹
	 * @param _posList PDF��Υե�����ʥե�����ɡˤΥꥹ��
	 * @param _imageFile ����
	 * @param int _style ����ɽ������
	 * @throws DocumentException
	 */
	private void setImage(PdfStamper _stamper,
						  List<AcroFields.FieldPosition> _posList,
						  Image _img,
						  int _style)
		throws DocumentException
	{
		PdfContentByte content = null;
		AcroFields.FieldPosition pos = null;

		float w = 0, h = 0;
		BigDecimal bdFrameH = null, bdFrameW = null, bdImageH = null, bdImageW = null, ratioH = null, ratioW = null;

		Image img = null;
		// Ʊ̾�Υ������åȤ�ʣ��������Ϥ��٤Ƥ�ȿ�Ǥ���
		int posListSize = _posList.size();
		for (int i = 0; i < posListSize; i++) {
			img = Image.getInstance(_img);
			pos = _posList.get(i);
			content = _stamper.getOverContent(pos.page);

			// ����ɽ�������ˤ��ʬ��
			switch (_style) {

				// �Ȥ˥ԥå�������
				case FIT_FRAME:
					w = pos.position.getWidth();
					h = pos.position.getHeight();
					break;

				// �Ȥι⤵�˹�碌��
				case FIT_HEIGHT:
					// �Ȥι⤵
					bdFrameH = new BigDecimal((long)pos.position.getHeight());
					bdFrameH = bdFrameH.setScale(10);
					// �����ι⤵
					bdImageH = new BigDecimal((long)img.getPlainHeight());
					bdImageH = bdImageH.setScale(10);
					// �Ȥι⤵�˹�碌�뤿�����Ψ
					ratioH = (bdFrameH.divide(bdImageH, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// ����������Ĵ��
					img.scalePercent(ratioH.floatValue());
					w = img.getPlainWidth();	// ������Ĵ�������
					h = img.getPlainHeight();	// ������Ĵ����ι⤵
					break;

				// �������ѹ����ʤ�
				case ORIGINAL:
					// PDF(72dpi)��Windows(96dpi)�β��̲����٤ΰ㤤������
					img.scalePercent(72.0f / 96.0f * 100f);
					w = img.getPlainWidth();
					h = img.getPlainHeight();
					break;

				// �Ȥ����˹�碌��
				case FIT_WIDTH:
					// �Ȥ���
					bdFrameW = new BigDecimal((long)pos.position.getWidth());
					bdFrameW = bdFrameW.setScale(10);
					// ��������
					bdImageW = new BigDecimal((long)img.getPlainWidth());
					bdImageW = bdImageW.setScale(10);
					// �Ȥ����˹�碌�뤿�����Ψ
					ratioW = (bdFrameW.divide(bdImageW, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// ����������Ĵ��
					img.scalePercent(ratioW.floatValue());
					w = img.getPlainWidth();	// ������Ĵ�������
					h = img.getPlainHeight();	// ������Ĵ����ι⤵
					break;

				// �Ĳ����ݻ���������˼����ʥǥե���Ȥ⤳�Υ����ס�
				case KEEP_WH_RATIO:
				default:
					// �Ȥ���
					bdFrameW = new BigDecimal((long)pos.position.getWidth());
					bdFrameW = bdFrameW.setScale(10);
					// ��������
					bdImageW = new BigDecimal((long)img.getPlainWidth());
					bdImageW = bdImageW.setScale(10);
					// �Ȥ����˹�碌���Ȥ�����Ψ
					ratioW = (bdFrameW.divide(bdImageW, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// �Ȥι⤵
					bdFrameH = new BigDecimal((long)pos.position.getHeight());
					bdFrameH = bdFrameH.setScale(10);
					// �����ι⤵
					bdImageH = new BigDecimal((long)img.getPlainHeight());
					bdImageH = bdImageH.setScale(10);
					// �Ȥι⤵�˹�碌���Ȥ�����Ψ
					ratioH = (bdFrameH.divide(bdImageH, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// ���������˹�碌��
					if (ratioW.compareTo(ratioH) < 1) {
						img.scalePercent(ratioW.floatValue());
					} else {
						img.scalePercent(ratioH.floatValue());
					}
					w = img.getPlainWidth();	// ������Ĵ�������
					h = img.getPlainHeight();	// ������Ĵ����ι⤵
					break;
			}

			// To position an image at (x,y) use addImage(image, image_width, 0, 0, image_height, x, y)
			content.addImage(img, w, 0, 0, h, pos.position.getLeft(), pos.position.getBottom());
		}
	}

	/**
	 * QR�����ɥ��å�.
	 * QR�����ɤΥ������ϥ������åȤΥ������˰�¸�ʽĲ����������˹�碌���������������
	 * ���ִ����֤ϥ������åȤκ���
	 *
	 * @param _stamper �ƥ�ץ��Խ����饹
	 * @param _posList PDF��Υե�����ʥե�����ɡˤΥꥹ��
	 * @param _value �����ɲ�����ʸ����
	 * @param _lv ���������٥�
	 * @throws DocumentException
	 */
	private void setQR(PdfStamper _stamper,
					   List<AcroFields.FieldPosition> _posList,
					   String _value,
					   int _lv)
		throws DocumentException, UnsupportedEncodingException
	{
		AcroFields.FieldPosition pos = null;
		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
		hints.put(EncodeHintType.CHARACTER_SET, "ISO-8859-1");

		// ���������٥�
		switch (_lv) {
			// 7%
			case LV_L:
				hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
				break;
			// 15%
			case LV_M:
				hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
				break;
			// 25%
			case LV_Q:
				hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
				break;
			// 30%
			case LV_H:
				hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
				break;
			// �ǥե���Ȥ�15%
			default:
				hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
				break;
		}

		int min = 0;
		int posW = 0;
		int posH = 0;
		BarcodeQRCode qrCode = null;
		Image qrImg = null;
		PdfContentByte content = null;

		// Ʊ̾�Υ������åȤ�ʣ��������Ϥ��٤Ƥ�ȿ�Ǥ���
		int posListSize = _posList.size();
		for (int i = 0; i < posListSize; i++) {
			pos = _posList.get(i);
			posW = (int)pos.position.getWidth();	// �������åȤ���
			posH = (int)pos.position.getHeight();	// �������åȤι⤵

			// �����⤵���������������
			if (posW < posH) {
				min = posW;
			} else {
				min = posH;
			}

			// QR�����ɤΥ��󥹥��󥹤�����
			qrCode = new BarcodeQRCode(U2S(_value), min, min, hints);

			// QR�����ɤ�����Ȥ��Ƽ���
			qrImg = qrCode.getImage();
			content = _stamper.getOverContent(pos.page);
			// PDF��˲�������
			content.addImage(qrImg, min, 0, 0, min, pos.position.getLeft(), pos.position.getBottom());
		}
	}

	/**
	 * Unicodeʸ�����Windows-31Jʸ������Ѵ�
	 */
	public static String U2S(String _str)
	{
		if (_str == null || _str.length() == 0) {
			return _str;
		}

		try {
			char[] c = _str.toCharArray();
			for (int i = 0; i < c.length; i++) {
				char d = c[i];
				// �ȥ��å�������ѥ�������Ѵ�
				if (d == '\u301c') {
					c[i] = '\uff5e';
				}
			}
			String str = new String(c);
			return new String(str.getBytes("Windows-31J"), "ISO-8859-1");
		} catch (Exception e) {
			return _str;
		}
	}
}

package pdf_generator;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.exceptions.BadPasswordException;

/**
 * PDF��Ϣ�Υ桼�ƥ���ƥ����饹
 */
public final class PdfUtil
{
	// ���ڷ��
	public static final int FAILED			= -99;	// ���ڼ���
	public static final int INVALID_PDF		= -1;	// ̵����PDF
	public static final int IS_PDF			= 0;	// PDF�Ǥ���ʥѥ��������ʤ���
	public static final int HAS_OWN_PASS	= 1;	// �����ʡ��ѥ���ɤ����ꤵ�줿PDF
	public static final int HAS_USR_PASS	= 2;	// �桼�����ѥ���ɤ����ꤵ�줿PDF
	public static final int HAS_PASS		= 3;	// �ѥ�����㳰
	public static final int HAS_CERT		= 4;	// ������ˤ�륻�����ƥ��ݸ�줿PDF

	/**
	 * PDF�ե�����򸡾ڤ�����̤��ֵ�
	 *
	 * @param _pdf PDF��byte����
	 * @return  ���ڷ��
	 */
	public static final int checkPdf(byte[] _pdf)
	{
		try {
			// ���������å�
			if (_pdf == null || _pdf.length < 1) {
				throw new Exception("pdf is null or size 0.");
			}

			// �����Ƥߤ�
			PdfReader reader = new PdfReader(_pdf);

			ByteArrayOutputStream byteout = new ByteArrayOutputStream();

			// �Խ����ߤ�
			PdfStamper stamper = new PdfStamper(reader, byteout);

			return IS_PDF;

		// ̵����PDF
		} catch (InvalidPdfException e) {
			e.printStackTrace();
			if ((e.getMessage()).indexOf("Bad certificate") != -1) {
				return HAS_CERT;
			}
			return INVALID_PDF;

		// �ѥ���ɤ����ꤵ��Ƥ���
		} catch (BadPasswordException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (msg.indexOf("owner password") != -1) {
				return HAS_OWN_PASS;
			}
			if (msg.indexOf("user password") != -1) {
				return HAS_USR_PASS;
			}
			return HAS_PASS;

		// ����¾IO���顼
		} catch (IOException e) {
			e.printStackTrace();
			return FAILED;

		// ���������å��ǤΥ��顼
		} catch (Exception e) {
			e.printStackTrace();
			return FAILED;
		}
	}
}


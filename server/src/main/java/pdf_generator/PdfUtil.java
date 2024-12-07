package pdf_generator;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.exceptions.BadPasswordException;

/**
 * PDF関連のユーティリティクラス
 */
public final class PdfUtil
{
	// 検証結果
	public static final int FAILED			= -99;	// 検証失敗
	public static final int INVALID_PDF		= -1;	// 無効なPDF
	public static final int IS_PDF			= 0;	// PDFである（パスワード設定なし）
	public static final int HAS_OWN_PASS	= 1;	// オーナーパスワードが設定されたPDF
	public static final int HAS_USR_PASS	= 2;	// ユーザーパスワードが設定されたPDF
	public static final int HAS_PASS		= 3;	// パスワード例外
	public static final int HAS_CERT		= 4;	// 証明書によるセキュリティ保護されたPDF

	/**
	 * PDFファイルを検証し、結果を返却
	 *
	 * @param _pdf PDFのbyte配列
	 * @return  検証結果
	 */
	public static final int checkPdf(byte[] _pdf)
	{
		try {
			// 引数チェック
			if (_pdf == null || _pdf.length < 1) {
				throw new Exception("pdf is null or size 0.");
			}

			// 開いてみる
			PdfReader reader = new PdfReader(_pdf);

			ByteArrayOutputStream byteout = new ByteArrayOutputStream();

			// 編集を試みる
			PdfStamper stamper = new PdfStamper(reader, byteout);

			return IS_PDF;

		// 無効なPDF
		} catch (InvalidPdfException e) {
			e.printStackTrace();
			if ((e.getMessage()).indexOf("Bad certificate") != -1) {
				return HAS_CERT;
			}
			return INVALID_PDF;

		// パスワードが設定されている
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

		// その他IOエラー
		} catch (IOException e) {
			e.printStackTrace();
			return FAILED;

		// 引数チェックでのエラー
		} catch (Exception e) {
			e.printStackTrace();
			return FAILED;
		}
	}
}


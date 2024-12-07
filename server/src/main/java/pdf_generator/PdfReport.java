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
 * PDF帳票用のクラス
 */
public class PdfReport
{
	// データ出力形式
	public static final int OUTPUT_VALUE	= 1;	// 値
	public static final int OUTPUT_IMAGE	= 2;	// 画像
	public static final int OUTPUT_QR		= 3;	// QRコード

	// 画像出力形式
	public static final int KEEP_WH_RATIO	= 1;	// 縦横比を維持して枠内に収める
	public static final int FIT_FRAME		= 2;	// 枠にピッタリ収める
	public static final int FIT_HEIGHT		= 3;	// 枠の高さに合わせて
	public static final int FIT_WIDTH		= 4;	// 枠の幅に合わせて
	public static final int ORIGINAL		= 5;	// そのまま

	// QRコード誤り訂正レベル
	public static final int LV_L =	1;	//  7%
	public static final int LV_M =	2;	// 15%
	public static final int LV_Q =	3;	// 25%
	public static final int LV_H =	4;	// 30%

	// 暗号化方式
	public static final int ENC_RC4_40	= 1;	// 40-bit RC4
	public static final int ENC_RC4_128	= 2;	// 128-bit RC4
	public static final int ENC_AES_128	= 3;	// 128-bit AES
	public static final int ENC_AES_256	= 4;	// 256-bit AES

	// 制限
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
	 * コンストラクタ
	 *
	 * @param _template PDFテンプレート
	 * @param _settings PDFの設定
	 */
	public PdfReport(byte[] _template,
					 JSONObject _settings)
		throws Exception
	{
		// 引数チェック
		if (_template == null) {
			throw new Exception("_template is null.");
		}
		if (_settings == null) {
			throw new Exception("_settings is null.");
		}

		// テンプレート
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
	 * PDF帳票出力
	 *
	 * @param _res サーブレットレスポンス
	 */
	public void output(HttpServletResponse _res)
		throws IOException, DocumentException
	{
		ServletOutputStream objOut = null;

		try {
			// 出力用のStreamをインスタンス化
			ByteArrayOutputStream byteout = new ByteArrayOutputStream();

			// テンプレート編集用
			PdfStamper stamper = new PdfStamper(m_reader, byteout);

			// パスワード設定がある場合
			if ((m_usrPass != null && m_usrPass.length() > 0) || (m_ownPass != null && m_ownPass.length() > 0)) {
				// ユーザーパスはあってオーナーパスが無い場合はオーナーパスをユーザーパスと同一化
				if (m_usrPass != null && m_usrPass.length() > 0 && (m_ownPass == null || m_ownPass.length() < 1)) {
					m_ownPass = m_usrPass;
				}

				// PDFを暗号化
				stamper.setEncryption(m_encryption, m_usrPass, m_ownPass, m_permission);
			}

			// テンプレート内のフィールド群を取得
			AcroFields fields = stamper.getAcroFields();

			// フィールドにデータをセット
			setData(stamper, fields);

			// フォームの平面化（編集不可化）
			stamper.setFormFlattening(true);

			// 編集完了
			stamper.close();

			// PDFをバイナリデータとしてクライアントに出力
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
	 * テンプレート内へデータセット
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

		// データリストのサイズ分だけループ
		for (int i = 0; i < listSize; i++) {
			// データをJSON形式で取得
			jsonData = JSONObject.fromObject(m_dataList.get(i));

			// データ出力形式を取得
			oTypeStr = jsonData.getString("output_type");
			if (oTypeStr != null) {
				oType = Integer.parseInt(oTypeStr);
			}

			// ターゲット名（テンプレ内に設置されたフォーム名）取得
			tgtName = jsonData.getString("target_name");

			// ターゲットに当て込む値
			value = jsonData.getString("value");

			// ターゲットがテキストフィールドでなければ何もしない
			fieldType = _fields.getFieldType(tgtName);
			if (fieldType != AcroFields.FIELD_TYPE_TEXT) {
				continue;
			}

			// データ出力形式により分岐
			switch (oType) {

				// 値をそのまま表示
				case OUTPUT_VALUE:
					_fields.setField(tgtName, value);
					break;

				// 画像として表示
				case OUTPUT_IMAGE:
					imgStyle = Integer.parseInt(jsonData.getString("image_style"));
					img = Image.getInstance(value.getBytes());
					posList = _fields.getFieldPositions(tgtName);
					setImage(_stamper, posList, img, imgStyle);
					break;

				// QRコードとして表示
				case OUTPUT_QR:
					corrLv = Integer.parseInt(jsonData.getString("correction_lv"));
					posList = _fields.getFieldPositions(tgtName);
					setQR(_stamper, posList, value, corrLv);
					break;

				// デフォルト
				default:
					break;
			}
		}
	}

	/**
	 * 画像セット
	 *
	 * @param _stamper テンプレート編集用クラス
	 * @param _posList PDF内のフォーム（フィールド）のリスト
	 * @param _imageFile 画像
	 * @param int _style 画像表示形式
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
		// 同名のターゲットが複数ある場合はすべてに反映する
		int posListSize = _posList.size();
		for (int i = 0; i < posListSize; i++) {
			img = Image.getInstance(_img);
			pos = _posList.get(i);
			content = _stamper.getOverContent(pos.page);

			// 画像表示形式により分岐
			switch (_style) {

				// 枠にピッタリ収める
				case FIT_FRAME:
					w = pos.position.getWidth();
					h = pos.position.getHeight();
					break;

				// 枠の高さに合わせる
				case FIT_HEIGHT:
					// 枠の高さ
					bdFrameH = new BigDecimal((long)pos.position.getHeight());
					bdFrameH = bdFrameH.setScale(10);
					// 画像の高さ
					bdImageH = new BigDecimal((long)img.getPlainHeight());
					bdImageH = bdImageH.setScale(10);
					// 枠の高さに合わせるための倍率
					ratioH = (bdFrameH.divide(bdImageH, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// 画像サイズ調整
					img.scalePercent(ratioH.floatValue());
					w = img.getPlainWidth();	// サイズ調整後の幅
					h = img.getPlainHeight();	// サイズ調整後の高さ
					break;

				// サイズ変更しない
				case ORIGINAL:
					// PDF(72dpi)とWindows(96dpi)の画面解像度の違いを補正
					img.scalePercent(72.0f / 96.0f * 100f);
					w = img.getPlainWidth();
					h = img.getPlainHeight();
					break;

				// 枠の幅に合わせる
				case FIT_WIDTH:
					// 枠の幅
					bdFrameW = new BigDecimal((long)pos.position.getWidth());
					bdFrameW = bdFrameW.setScale(10);
					// 画像の幅
					bdImageW = new BigDecimal((long)img.getPlainWidth());
					bdImageW = bdImageW.setScale(10);
					// 枠の幅に合わせるための倍率
					ratioW = (bdFrameW.divide(bdImageW, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// 画像サイズ調整
					img.scalePercent(ratioW.floatValue());
					w = img.getPlainWidth();	// サイズ調整後の幅
					h = img.getPlainHeight();	// サイズ調整後の高さ
					break;

				// 縦横比を維持して枠内に収める（デフォルトもこのタイプ）
				case KEEP_WH_RATIO:
				default:
					// 枠の幅
					bdFrameW = new BigDecimal((long)pos.position.getWidth());
					bdFrameW = bdFrameW.setScale(10);
					// 画像の幅
					bdImageW = new BigDecimal((long)img.getPlainWidth());
					bdImageW = bdImageW.setScale(10);
					// 枠の幅に合わせたときの倍率
					ratioW = (bdFrameW.divide(bdImageW, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// 枠の高さ
					bdFrameH = new BigDecimal((long)pos.position.getHeight());
					bdFrameH = bdFrameH.setScale(10);
					// 画像の高さ
					bdImageH = new BigDecimal((long)img.getPlainHeight());
					bdImageH = bdImageH.setScale(10);
					// 枠の高さに合わせたときの倍率
					ratioH = (bdFrameH.divide(bdImageH, BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100));
					// 小さい方に合わせる
					if (ratioW.compareTo(ratioH) < 1) {
						img.scalePercent(ratioW.floatValue());
					} else {
						img.scalePercent(ratioH.floatValue());
					}
					w = img.getPlainWidth();	// サイズ調整後の幅
					h = img.getPlainHeight();	// サイズ調整後の高さ
					break;
			}

			// To position an image at (x,y) use addImage(image, image_width, 0, 0, image_height, x, y)
			content.addImage(img, w, 0, 0, h, pos.position.getLeft(), pos.position.getBottom());
		}
	}

	/**
	 * QRコードセット.
	 * QRコードのサイズはターゲットのサイズに依存（縦横小さい方に合わせて正方形を形成）
	 * 設置基準位置はターゲットの左下
	 *
	 * @param _stamper テンプレ編集クラス
	 * @param _posList PDF内のフォーム（フィールド）のリスト
	 * @param _value コード化する文字列
	 * @param _lv 誤り訂正レベル
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

		// 誤り訂正レベル
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
			// デフォルトは15%
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

		// 同名のターゲットが複数ある場合はすべてに反映する
		int posListSize = _posList.size();
		for (int i = 0; i < posListSize; i++) {
			pos = _posList.get(i);
			posW = (int)pos.position.getWidth();	// ターゲットの幅
			posH = (int)pos.position.getHeight();	// ターゲットの高さ

			// 幅、高さ、小さい方を採用
			if (posW < posH) {
				min = posW;
			} else {
				min = posH;
			}

			// QRコードのインスタンスを生成
			qrCode = new BarcodeQRCode(U2S(_value), min, min, hints);

			// QRコードを画像として取得
			qrImg = qrCode.getImage();
			content = _stamper.getOverContent(pos.page);
			// PDF内に画像設置
			content.addImage(qrImg, min, 0, 0, min, pos.position.getLeft(), pos.position.getBottom());
		}
	}

	/**
	 * Unicode文字列をWindows-31J文字列に変換
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
				// 波ダッシュは全角チルダに変換
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

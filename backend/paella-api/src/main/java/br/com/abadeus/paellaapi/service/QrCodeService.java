package br.com.abadeus.paellaapi.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/** Gera a imagem PNG do QR Code usando a biblioteca ZXing. */
@Service
public class QrCodeService {

    public byte[] gerarPng(String conteudo, int tamanho) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho, hints);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", saida);
            return saida.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Falha ao gerar QR Code", e);
        }
    }

    public String gerarBase64(String conteudo, int tamanho) {
        return Base64.getEncoder().encodeToString(gerarPng(conteudo, tamanho));
    }
}

package ru.iguana.deal.api.service.validation;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Извлекает «сырой» текстовый слой из PDF-документа с помощью Apache PDFBox.
 *
 * <p>Сервис намеренно тонкий: возвращает строку, пригодную для дальнейшего
 * разбора регулярными выражениями в {@link PdfFieldParser}. Если PDF
 * повреждён или зашифрован, метод бросает {@link PdfExtractionException}
 * с человекочитаемой причиной — выше по стеку в
 * {@code DocumentValidationService} это будет завернуто в ошибку
 * валидации {@code PDF_PARSE_ERROR}.
 */
@Component
@Slf4j
public class PdfTextExtractor {

    /**
     * Извлекает текст из PDF-байтов.
     *
     * @param content бинарное содержимое PDF (BYTEA-колонка {@code user_document.content})
     * @return распознанный текст; никогда не {@code null}
     * @throws PdfExtractionException если PDF не удалось открыть/распарсить
     */
    public String extractText(byte[] content) {
        if (content == null || content.length == 0) {
            log.warn("PdfTextExtractor: пустой PDF (content == null или 0 байт)");
            throw new PdfExtractionException("PDF пуст");
        }

        log.debug("PdfTextExtractor: открываю PDF, размер = {} байт", content.length);

        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                log.warn("PdfTextExtractor: документ зашифрован — извлечение невозможно");
                throw new PdfExtractionException("PDF зашифрован");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            log.debug("PdfTextExtractor: извлечено символов = {}, страниц = {}",
                    text.length(), document.getNumberOfPages());
            return text;
        } catch (IOException ex) {
            log.error("PdfTextExtractor: не удалось распарсить PDF: {}", ex.getMessage(), ex);
            throw new PdfExtractionException("Не удалось распарсить PDF: " + ex.getMessage(), ex);
        }
    }

    /**
     * Исключение этапа извлечения текста из PDF. Конвертируется в
     * {@link ru.iguana.deal.model.entity.enums.ValidationErrorType#PDF_PARSE_ERROR}.
     */
    public static class PdfExtractionException extends RuntimeException {
        public PdfExtractionException(String message) {
            super(message);
        }

        public PdfExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

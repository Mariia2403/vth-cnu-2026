package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception;

// "Порушено бізнес-правило". code — коротка назва порушення (напр. "INVALID_STATS"),
// за нею контролер потім вирішить, який HTTP-код повернути.
public class DomainException extends RuntimeException {
    private final String code;

    public DomainException(String code, String message) {
        super(message);       // передаємо текст помилки батьківському класу
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
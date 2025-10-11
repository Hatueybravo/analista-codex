"""Utilities for turning PDF files into plain text.

This module tries to use `PyMuPDF` (``fitz``) when it is available because it
provides excellent results for both text based and scanned PDFs.  When PyMuPDF
is not installed we transparently fall back to ``PyPDF2`` which is already a
dependency of the project.  The goal is to keep ``procesar_pdfs`` working on a
fresh Windows installation without requiring additional packages from the
user.

The ``ocr_pdf_to_text`` helper raises :class:`OCRDependencyError` when neither
backend is present so callers can surface a clear, actionable message to the
user.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Protocol


class TextExtractor(Protocol):
    def __call__(self, pdf_path: Path) -> str:  # pragma: no cover - Protocol stub
        ...


class OCRDependencyError(RuntimeError):
    """Raised when no backend is available to extract text from a PDF."""


def _load_pymupdf() -> TextExtractor | None:
    try:
        import fitz  # type: ignore
    except ModuleNotFoundError:
        return None

    def extractor(pdf_path: Path) -> str:
        doc = fitz.open(pdf_path)  # type: ignore[attr-defined]
        try:
            texts = []
            for page in doc:
                texts.append(page.get_text())
            return "\n".join(texts)
        finally:
            doc.close()

    return extractor


def _load_pypdf2() -> TextExtractor | None:
    try:
        from PyPDF2 import PdfReader
    except ModuleNotFoundError:
        return None

    def extractor(pdf_path: Path) -> str:
        reader = PdfReader(str(pdf_path))
        texts = []
        for page in reader.pages:
            texts.append(page.extract_text() or "")
        return "\n".join(texts)

    return extractor


@dataclass(slots=True)
class _ExtractorSuite:
    preferred: TextExtractor | None
    fallback: TextExtractor | None

    def extract(self, pdf_path: Path) -> str:
        if self.preferred is not None:
            try:
                return self.preferred(pdf_path)
            except Exception:
                # Fallback to the alternative backend if the primary one fails
                pass

        if self.fallback is not None:
            return self.fallback(pdf_path)

        raise OCRDependencyError(
            "No se encontró un motor de extracción de texto. Instala PyMuPDF "
            "('pip install pymupdf') o PyPDF2 para continuar."
        )


_SUITE = _ExtractorSuite(
    preferred=_load_pymupdf(),
    fallback=_load_pypdf2(),
)


def ocr_pdf_to_text(pdf_path: str | Path) -> str:
    """Return the extracted text from ``pdf_path``.

    Parameters
    ----------
    pdf_path:
        Path to the PDF file.  The file must exist.
    """

    path = Path(pdf_path)
    if not path.exists():
        raise FileNotFoundError(f"No se encontró el archivo PDF: {path}")

    return _SUITE.extract(path)


__all__ = ["ocr_pdf_to_text", "OCRDependencyError"]


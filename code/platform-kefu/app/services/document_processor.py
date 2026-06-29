import fitz
from docx import Document
from pathlib import Path

class DocumentProcessor:
    def __init__(self, upload_dir: Path):
        self.upload_dir = upload_dir
        self.upload_dir.mkdir(parents=True, exist_ok=True)

    def extract_text(self, file_path: Path, ext: str) -> str:
        if ext == ".pdf":
            return self._extract_pdf(file_path)
        elif ext == ".docx":
            return self._extract_docx(file_path)
        elif ext == ".md" or ext == ".txt":
            return file_path.read_text(encoding="utf-8", errors="ignore")
        else:
            raise ValueError(f"Unsupported file type: {ext}")

    def _extract_pdf(self, path: Path) -> str:
        text = ""
        doc = fitz.open(str(path))
        for page in doc:
            text += page.get_text()
        doc.close()
        return text

    def _extract_docx(self, path: Path) -> str:
        doc = Document(str(path))
        return "\n".join(p.text for p in doc.paragraphs)
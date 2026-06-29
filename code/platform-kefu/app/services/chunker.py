import re

class Chunker:
    def __init__(self, chunk_size: int = 512, overlap: int = 64):
        self.chunk_size = chunk_size
        self.overlap = overlap

    def split(self, text: str) -> list:
        paragraphs = re.split(r'\n\s*\n', text.strip())
        chunks = []
        current = ""
        for para in paragraphs:
            para = para.strip()
            if not para:
                continue
            words = para.split()
            if len(current.split()) + len(words) <= self.chunk_size:
                current = (current + "\n\n" + para).strip()
            else:
                if current:
                    chunks.append(current)
                current = para
        if current:
            chunks.append(current)
        return chunks

    def estimate_tokens(self, text: str) -> int:
        return len(text.split())
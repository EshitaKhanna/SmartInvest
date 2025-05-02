from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

class SentimentAnalyzer:
    def __init__(self):
        self.tokenizer = AutoTokenizer.from_pretrained("ProsusAI/finbert")
        self.model = AutoModelForSequenceClassification.from_pretrained("ProsusAI/finbert")

    def analyze_sentiment(self, text):
        if not text or not isinstance(text, str):
            raise ValueError("Text must be a non-empty string")
        
        inputs = self.tokenizer(text, return_tensors="pt", truncation=True, padding=True)
        outputs = self.model(**inputs)
        probs = torch.nn.functional.softmax(outputs.logits, dim=-1)
        sentiment = torch.argmax(probs).item()  # 0: neutral, 1: positive, 2: negative
        return probs

    def analyze_article_sentiment(self, title, content):
        if not title or not isinstance(title, str):
            raise ValueError("Title must be a non-empty string")
        if not content or not isinstance(content, str):
            print(f"Skipping article with empty content: {title}")
            return None  # Skip this article
        
        title_probs = self.analyze_sentiment(title)
        content_probs = self.analyze_sentiment(content)

        # Weighted sentiment: 40% title, 60% content
        combined_probs = (0.4 * title_probs) + (0.6 * content_probs)
        sentiment = torch.argmax(combined_probs).item() # 0: neutral, 1: positive, 2: negative
        return {0: 0.5, 1: 1.0, 2: 0.0}.get(sentiment, 0.5)  # map sentiment to score

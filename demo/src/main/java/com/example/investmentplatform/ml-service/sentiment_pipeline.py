import sys
from news_fetcher import NewsFetcher
from sentiment_analyzer import SentimentAnalyzer
from database_handler import DatabaseHandler
import time

class SentimentPipeline:
    def __init__(self, newsapi_key, dbname, user, host="localhost"):
        self.news_fetcher = NewsFetcher(newsapi_key)
        self.sentiment_analyzer = SentimentAnalyzer()
        self.db_handler = DatabaseHandler(dbname, user, host)

    def run(self, stock_symbol):
        try:
            articles = self.news_fetcher.fetch_all_articles(stock_symbol)
            total_score = 0
            valid_articles = 0

            for article in articles:
                try:
                    title = article["title"]
                    content = article["content"]
                    if not content:
                        continue

                    score = self.sentiment_analyzer.analyze_article_sentiment(title, content)
                    if score is not None:
                        total_score += score
                        valid_articles += 1

                    # Log to stderr instead of stdout
                    print(f"Processed: {title} | Score: {score}", file=sys.stderr)
            
                except Exception as e:
                        print(f"Error processing article: {str(e)}", file=sys.stderr)   
                
            # Calculate final score
            avg_score = total_score / valid_articles if valid_articles > 0 else 0.5
            print(avg_score, flush = True)  # ONLY numerical output to stdout

        except Exception as e:
            print(0.5, flush=True) # Error fallback to stdout
            print(f"Critical error: {str(e)}", file=sys.stderr)
            sys.exit(1)
    
    def run_and_return(self, stock_symbol):
        try:
            articles = self.news_fetcher.fetch_all_articles(stock_symbol)
            total_score, valid_articles = 0, 0
            for article in articles:
                try:

                    title = article["title"]
                    content = article["content"]
                    if not title or not content:
                        continue

                    score = self.sentiment_analyzer.analyze_article_sentiment(title, content)
                    if score is not None:
                        self.db_handler.save_sentiment_data(stock_symbol, title, content, score)
                        total_score += score
                        valid_articles += 1
                except Exception as e:
                    print(f"Error processing article: {str(e)}", file=sys.stderr)
                    continue

            return total_score / valid_articles if valid_articles > 0 else 0.5

        except Exception as e:
            print(f"Pipeline error: {str(e)}", file=sys.stderr)
            return 0.5  # Fallback value


    def close(self):
        self.db_handler.close()

if __name__ == "__main__":
    # Configuration
    NEWSAPI_KEY = "5bf4203217d340019440f212578d1dbc"
    DB_NAME = "investment_platform"
    DB_USER = "Eshita"
    
    # Initialize and run pipeline
    pipeline = SentimentPipeline(NEWSAPI_KEY, DB_NAME, DB_USER)

    # Check if a stock symbol is provided as a command-line argument
    if len(sys.argv) != 2:
        print("Usage: python sentiment_pipeline.py <stock_symbol>", file=sys.stderr)
        sys.exit(1)

    stock_symbol = sys.argv[1]

    try:
        pipeline.run(stock_symbol)
        # time.sleep(3600)  # Run every hour
    except KeyboardInterrupt:
        print("Pipeline stopped.", file=sys.stderr)
    finally:
        pipeline.close()
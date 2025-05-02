import sys
import requests
from GoogleNews import GoogleNews
from bs4 import BeautifulSoup
from datetime import datetime, timedelta

class NewsFetcher:
    def __init__(self, newsapi_key):
        self.newsapi_key = newsapi_key

    def fetch_newsapi_articles(self, query="stocks"):
        # # Calculate the date range (past 14 days)
        # end_date = datetime.utcnow()
        # start_date = end_date - timedelta(days=14)

        # # Format dates for the API request
        # start_date_str = start_date.strftime("%Y-%m-%d")
        # end_date_str = end_date.strftime("%Y-%m-%d")

        # Fetch articles from NewsAPI
        url = f"https://newsapi.org/v2/everything?q={query}&pageSize=14&apiKey={self.newsapi_key}"
        response = requests.get(url)
        if response.status_code == 200:
            articles = response.json().get("articles", [])[:14]
            return [{"title": article["title"], "content": article["description"], "published_at": article["publishedAt"]} for article in articles]
        else:
            print(f"Error fetching NewsAPI data: {response.status_code}", file=sys.stderr)
            return []

    def fetch_google_news_articles(self, query="stocks"):
        googlenews = GoogleNews()
        googlenews.search(query)
        googlenews.get_page(1)
        articles = []
        for article in googlenews.result()[:14]:
            title = article["title"]
            content = article["desc"]
            published_at = article["date"]  # Relative time string (e.g., "3 minutes ago")
            try:
                published_at_dt = self.parse_relative_time(published_at)
                articles.append({"title": title, "content": content, "published_at": published_at_dt})
            except ValueError as e:
                print(f"Skipping article due to invalid date format: {e}", file=sys.stderr)
        googlenews.clear()
        return articles

    def fetch_yahoo_news_articles(self, query="stocks"):
        url = f"https://news.search.yahoo.com/search?p={query}"
        response = requests.get(url)
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, "html.parser")
            articles = []
            for a in soup.find_all("a", class_="fz-20 lh-26 fw-b"):
                title = a.text
                content = a.find_next("p", class_="fz-14 lh-20").text if a.find_next("p", class_="fz-14 lh-20") else ""
                published_at = a.find_next("span", class_="fc-2nd").text if a.find_next("span", class_="fc-2nd") else ""
                try:
                    published_at_dt = self.parse_relative_time(published_at)
                    articles.append({"title": title, "content": content, "published_at": published_at_dt})
                except ValueError as e:
                    print(f"Skipping article due to invalid date format: {e}", file=sys.stderr)
            return articles[:14]
        else:
            print(f"Error fetching Yahoo News data: {response.status_code}", file=sys.stderr)
            return []
        
    def fetch_all_articles(self, query="stocks"):
        newsapi_articles = self.fetch_newsapi_articles(query)
        google_articles = self.fetch_google_news_articles(query)
        yahoo_articles = self.fetch_yahoo_news_articles(query)

        # Debug: Print counts per source
        print(f"NewsAPI: {len(newsapi_articles)} articles", file=sys.stderr)
        print(f"Google News: {len(google_articles)} articles", file=sys.stderr)
        print(f"Yahoo News: {len(yahoo_articles)} articles", file=sys.stderr)
    
        all_articles = newsapi_articles + google_articles + yahoo_articles

        return self.filter_unique_articles(all_articles)[:42] 
    
    @staticmethod
    def filter_unique_articles(articles):
        unique_articles = []
        seen_titles = set()
        for article in articles:
            if article["title"] not in seen_titles:
                seen_titles.add(article["title"])
                unique_articles.append(article)
        return unique_articles
    
    @staticmethod
    def parse_relative_time(relative_time):
        """
        Convert relative time strings (e.g., "3 minutes ago") into datetime objects.
        """

        now = datetime.utcnow()
        try:
            if "minute" in relative_time:
                minutes = int(relative_time.split()[0])
                return now - timedelta(minutes=minutes)
            elif "hour" in relative_time:
                hours = int(relative_time.split()[0])
                return now - timedelta(hours=hours)
            elif "day" in relative_time:
                days = int(relative_time.split()[0])
                return now - timedelta(days=days)
            elif "week" in relative_time:
                weeks = int(relative_time.split()[0])
                return now - timedelta(weeks=weeks)
            else:
                raise ValueError(f"Unsupported relative time format: {relative_time}", file=sys.stderr)
        except:
            return now  # Return current time as fallback
if (!require('tidyverse')) install.packages('tidyverse'); 
if (!require('rtweet')) install.packages('rtweet'); 
if (!require('sentimentr')) install.packages('sentimentr');
if (!require('dplyr')) install.packages('dplyr');
if (!require('ggplot2')) install.packages('ggplot2');
if (!require('scales')) install.packages('scales');

library(tidyverse)
library(rtweet)
library(sentimentr)
library(dplyr)
library(ggplot2)
library(scales)

# # Check sentimentr working
# text <- "I am so motivated"
# sentiment(text)
# sentiment_by(text)
# sentiment_by(text, by = NULL)
# profanity(text)

# Get Tweets
tweets <- search_tweets(
  q = "Fauci", 
  n = 1000,
  type = "recent",
  lang = "en",
  include_rts = FALSE,
  verbose = TRUE
)

# Preprocess tweets
tweets$sentence <- gsub("http.*","",  tweets$text)
tweets$sentence <- gsub("https.*","", tweets$sentence)
tweets_clean <- tweets %>% dplyr::select(sentence)

# Get sentiment polarity of the tweets
tweets_with_pol <- tweets_clean %>% 
  get_sentences() %>% 
  sentiment() %>% 
  mutate(polarity = ifelse(sentiment < 0.2, "Negative", ifelse(sentiment > 0.2, "Positive", "Neutral")))
tweets_with_pol %>% View()
# tweets_with_pol %>% filter(polarity == "Negative") %>% View()
# tweets_with_pol %>% filter(polarity == "Positive") %>% View()
# tweets_with_pol %>% filter(polarity == "Neutral") %>% View()

# Extract Sentiment Keywords
tweets_with_pol$sentence %>%
  extract_sentiment_terms() %>% 
  dplyr::select(element_id, negative, positive, neutral)

# Highlight sentences based on sentiment
tweets_with_pol$sentence %>% 
  get_sentences() %>% 
  sentiment_by() %>%
  highlight()

# Change of sentiment over the tweets
ggplot(tweets_with_pol, aes(x=element_id, y=sentiment)) + 
  geom_point(aes(colour = factor(polarity)))
             
# Find the sentiment distribution
summary(tweets_with_pol$sentiment)

# Plot the average sentiment distribution
tweets_with_pol$sentence %>% 
  get_sentences() %>% 
  sentiment_by(by = NULL) %>%
  ggplot() + geom_density(aes(ave_sentiment))

# Remove outliers with sentiment values outside [-1,1]
tweets_with_pol %>% filter(between(sentiment,-1,1)) ->  tweets_with_pol
tweets_with_pol %>% View()

# Summarize the sentiment dataframe
dat <- with(density(tweets_with_pol$sentiment), data.frame(x, y))

# Plot the sentiment density
ggplot(dat, aes(x = x, y = y)) +
  geom_line() +
  geom_area(mapping = aes(x = ifelse(x >=0 & x<=1 , x, 0)), fill = "green") +
  geom_area(mapping = aes(x = ifelse(x <=0 & x>=-1 , x, 0)), fill = "red") +
  scale_y_continuous(limits = c(0,7.5)) +
  theme_minimal(base_size = 16) +
  labs(x = "Sentiment", 
       y = "", 
       title = "Sentiment distribution across the tweets") +
  theme(plot.title = element_text(hjust = 0.5), 
        axis.text.y=element_blank())

# Check spread of sentiment polarity
tweets_with_pol %>% 
  ggplot() + geom_boxplot(aes(x = polarity, y = element_id))

# Plot count of sentence of each polarity
ggplot(data=tweets_with_pol, aes(x=polarity)) +
  geom_bar() +
  geom_text(stat='count', aes(label=..count..), vjust=-1) +
  labs(y = "Count", x = "Sentence Polarity")

ggplot(tweets_with_pol, aes(x = as.factor(polarity))) +
  geom_bar(aes(y = (..count..)/sum(..count..))) +
  geom_text(aes(y = ((..count..)/sum(..count..)), 
                label = scales::percent((..count..)/sum(..count..))), 
            stat = "count", vjust = -0.25) +
  labs(y = "Percent", x = "Sentence Polarity")
